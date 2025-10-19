import torch
import torch.nn as nn
import torch.optim as optim
from torchvision import datasets, transforms, models
from torch.utils.data import Dataset, DataLoader, random_split
import os
from PIL import Image
import random

# -------- SETTINGS ----------
DATA_DIR = r"Garbage classification\Garbage classification"
BATCH_SIZE = 8
NUM_CLASSES = 6
NUM_EPOCHS_LAST_LAYER = 5  # first freeze conv layers
NUM_EPOCHS_FINE_TUNE = 20   # then fine-tune all layers
DEVICE = torch.device("cuda" if torch.cuda.is_available() else "cpu")
ONNX_EXPORT_PATH = "model.onnx"
CLASS_NAMES = ["cardboard", "glass", "metal", "paper", "plastic", "trash"]
IMG_SIZE = 224
VALID_PER_CLASS = 10  # validation images per class

# -------- DATA TRANSFORMS --------
train_transform = transforms.Compose([
    transforms.Resize((IMG_SIZE, IMG_SIZE)),
    transforms.RandomHorizontalFlip(),
    transforms.RandomRotation(15),
    transforms.ColorJitter(0.1, 0.1, 0.1, 0.1),
    transforms.ToTensor(),
    transforms.Normalize(mean=[0.485, 0.456, 0.406],
                         std=[0.229, 0.224, 0.225])
])

val_transform = transforms.Compose([
    transforms.Resize((IMG_SIZE, IMG_SIZE)),
    transforms.ToTensor(),
    transforms.Normalize(mean=[0.485, 0.456, 0.406],
                         std=[0.229, 0.224, 0.225])
])

# -------- CUSTOM DATASET --------
class UniqueImageDataset(Dataset):
    def __init__(self, root_dir, transform=None):
        self.transform = transform
        self.samples = []  # (image_path, label)
        for idx, class_name in enumerate(CLASS_NAMES):
            class_path = os.path.join(root_dir, class_name)
            if not os.path.isdir(class_path):
                continue
            seen_prefixes = set()
            for file_name in os.listdir(class_path):
                prefix = os.path.splitext(file_name)[0]
                if prefix not in seen_prefixes:
                    seen_prefixes.add(prefix)
                    self.samples.append((os.path.join(class_path, file_name), idx))

    def __len__(self):
        return len(self.samples)

    def __getitem__(self, idx):
        path, label = self.samples[idx]
        image = Image.open(path).convert("RGB")
        if self.transform:
            image = self.transform(image)
        return image, label

# -------- LOAD DATASET & SPLIT ----------
dataset = UniqueImageDataset(DATA_DIR, transform=train_transform)

# Create validation dataset: 10 images per class
val_samples = []
train_samples = []
class_counts = {cls: 0 for cls in range(NUM_CLASSES)}

random.shuffle(dataset.samples)
for path, label in dataset.samples:
    if class_counts[label] < VALID_PER_CLASS:
        val_samples.append((path, label))
        class_counts[label] += 1
    else:
        train_samples.append((path, label))

train_dataset = UniqueImageDataset(DATA_DIR, transform=train_transform)
train_dataset.samples = train_samples

val_dataset = UniqueImageDataset(DATA_DIR, transform=val_transform)
val_dataset.samples = val_samples

train_loader = DataLoader(train_dataset, batch_size=BATCH_SIZE, shuffle=True)
val_loader = DataLoader(val_dataset, batch_size=BATCH_SIZE, shuffle=False)

# -------- MODEL SETUP --------
model = models.resnet18(pretrained=True)
model.fc = nn.Linear(model.fc.in_features, NUM_CLASSES)
model = model.to(DEVICE)

# -------- LOSS & OPTIMIZER --------
criterion = nn.CrossEntropyLoss()

# -------- TRAIN LAST LAYER ONLY ----------
for param in model.parameters():
    param.requires_grad = False
for param in model.fc.parameters():
    param.requires_grad = True

optimizer = optim.Adam(model.fc.parameters(), lr=0.001)

print("Training last layer only...")
for epoch in range(NUM_EPOCHS_LAST_LAYER):
    model.train()
    running_loss = 0
    correct = 0
    total = 0
    for images, labels in train_loader:
        images, labels = images.to(DEVICE), labels.to(DEVICE)
        optimizer.zero_grad()
        outputs = model(images)
        loss = criterion(outputs, labels)
        loss.backward()
        optimizer.step()

        running_loss += loss.item()
        _, predicted = torch.max(outputs, 1)
        total += labels.size(0)
        correct += (predicted == labels).sum().item()

    # Validation
    model.eval()
    val_loss = 0
    val_correct = 0
    val_total = 0
    with torch.no_grad():
        for images, labels in val_loader:
            images, labels = images.to(DEVICE), labels.to(DEVICE)
            outputs = model(images)
            loss = criterion(outputs, labels)
            val_loss += loss.item()
            _, predicted = torch.max(outputs, 1)
            val_total += labels.size(0)
            val_correct += (predicted == labels).sum().item()

    print(f"Epoch {epoch+1}/{NUM_EPOCHS_LAST_LAYER}, "
          f"Train Loss: {running_loss/len(train_loader):.4f}, Train Acc: {100*correct/total:.2f}%, "
          f"Val Loss: {val_loss/len(val_loader):.4f}, Val Acc: {100*val_correct/val_total:.2f}%")

# -------- FINE-TUNE ALL LAYERS ----------
for param in model.parameters():
    param.requires_grad = True
optimizer = optim.Adam(model.parameters(), lr=1e-4)

print("Fine-tuning all layers...")
for epoch in range(NUM_EPOCHS_FINE_TUNE):
    model.train()
    running_loss = 0
    correct = 0
    total = 0
    for images, labels in train_loader:
        images, labels = images.to(DEVICE), labels.to(DEVICE)
        optimizer.zero_grad()
        outputs = model(images)
        loss = criterion(outputs, labels)
        loss.backward()
        optimizer.step()

        running_loss += loss.item()
        _, predicted = torch.max(outputs, 1)
        total += labels.size(0)
        correct += (predicted == labels).sum().item()

    # Validation
    model.eval()
    val_loss = 0
    val_correct = 0
    val_total = 0
    with torch.no_grad():
        for images, labels in val_loader:
            images, labels = images.to(DEVICE), labels.to(DEVICE)
            outputs = model(images)
            loss = criterion(outputs, labels)
            val_loss += loss.item()
            _, predicted = torch.max(outputs, 1)
            val_total += labels.size(0)
            val_correct += (predicted == labels).sum().item()

    print(f"Epoch {epoch+1}/{NUM_EPOCHS_FINE_TUNE}, "
          f"Train Loss: {running_loss/len(train_loader):.4f}, Train Acc: {100*correct/total:.2f}%, "
          f"Val Loss: {val_loss/len(val_loader):.4f}, Val Acc: {100*val_correct/val_total:.2f}%")

# -------- EXPORT TO ONNX ----------
dummy_input = torch.randn(1, 3, IMG_SIZE, IMG_SIZE, device=DEVICE)
torch.onnx.export(model, dummy_input, ONNX_EXPORT_PATH,
                  input_names=['input'], output_names=['output'],
                  opset_version=11, export_params=True)
print(f"✅ Model exported to {ONNX_EXPORT_PATH}")
