import onnxruntime as ort
import numpy as np
from PIL import Image
import os
from torchvision import transforms

# -------- SETTINGS --------
ONNX_MODEL_PATH = "model.onnx"
DATA_DIR = r"Garbage classification\Garbage classification"
CLASS_NAMES = ["cardboard", "glass", "metal", "paper", "plastic", "trash"]
IMG_SIZE = 224

# -------- TRANSFORMS --------
transform = transforms.Compose([
    transforms.Resize((IMG_SIZE, IMG_SIZE)),
    transforms.ToTensor(),
    transforms.Normalize(mean=[0.485, 0.456, 0.406],  # same as ImageNet
                         std=[0.229, 0.224, 0.225])
])

# -------- LOAD MODEL --------
session = ort.InferenceSession(ONNX_MODEL_PATH)
input_name = session.get_inputs()[0].name
output_name = session.get_outputs()[0].name

# -------- RUN INFERENCE ON ALL IMAGES --------
results = []

for class_name in CLASS_NAMES:
    class_path = os.path.join(DATA_DIR, class_name)
    if not os.path.isdir(class_path):
        continue

    for file_name in os.listdir(class_path):
        file_path = os.path.join(class_path, file_name)
        img = Image.open(file_path).convert("RGB")
        # Preprocess
        img_tensor = transform(img).unsqueeze(0).numpy().astype(np.float32)  # [1,3,224,224]

        # Run inference
        outputs = session.run([output_name], {input_name: img_tensor})
        logits = outputs[0][0]  # shape [NUM_CLASSES]
        # Softmax to get probabilities
        exp_logits = np.exp(logits - np.max(logits))  # for numerical stability
        probs = exp_logits / exp_logits.sum()
        pred_idx = np.argmax(probs)
        pred_class = CLASS_NAMES[pred_idx]

        # Print results
        print(f"{file_name}: True class = {class_name}, Predicted = {pred_class}, Confidence = {probs[pred_idx]:.4f}")

        # Optional: store for further analysis
        results.append({
            "file": file_name,
            "true_class": class_name,
            "pred_class": pred_class,
            "confidence": probs[pred_idx]
        })
