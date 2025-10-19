import cv2
import torch
import onnxruntime as ort
import numpy as np
from torchvision import transforms

# -------- SETTINGS ----------
MODEL_PATH = "model.onnx"
CLASS_NAMES = ["cardboard", "glass", "metal", "paper", "plastic", "trash"]
IMG_SIZE = 224
DEVICE = "cuda" if torch.cuda.is_available() else "cpu"

# -------- TRANSFORM --------
transform = transforms.Compose([
    transforms.ToPILImage(),
    transforms.Resize((IMG_SIZE, IMG_SIZE)),
    transforms.ToTensor(),
    transforms.Normalize(mean=[0.485, 0.456, 0.406],
                         std=[0.229, 0.224, 0.225])
])

# -------- LOAD MODEL --------
print("🔹 Loading ONNX model...")
session = ort.InferenceSession(MODEL_PATH, providers=["CUDAExecutionProvider", "CPUExecutionProvider"])
input_name = session.get_inputs()[0].name
output_name = session.get_outputs()[0].name
print("✅ Model loaded successfully!")

# -------- HELPER FUNCTION --------
def predict(frame):
    # Convert frame (BGR → RGB)
    img = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
    img = transform(img).unsqueeze(0).numpy()  # shape: (1, 3, 224, 224)

    # Run inference
    outputs = session.run([output_name], {input_name: img})[0]
    probs = torch.softmax(torch.tensor(outputs), dim=1)[0]
    pred_idx = torch.argmax(probs).item()
    confidence = probs[pred_idx].item()
    label = CLASS_NAMES[pred_idx]
    return label, confidence

# -------- REALTIME CAMERA LOOP --------
cap = cv2.VideoCapture(0)
if not cap.isOpened():
    print("❌ Cannot access camera.")
    exit()

print("🎥 Press 'q' to quit.")
while True:
    ret, frame = cap.read()
    if not ret:
        break

    # Predict
    label, conf = predict(frame)
    text = f"{label} ({conf*100:.1f}%)"

    # Overlay result
    cv2.putText(frame, text, (20, 40), cv2.FONT_HERSHEY_SIMPLEX,
                1.0, (0, 255, 0), 2, cv2.LINE_AA)
    cv2.imshow("Garbage Classifier - Realtime", frame)

    # Quit on 'q'
    if cv2.waitKey(1) & 0xFF == ord('q'):
        break

cap.release()
cv2.destroyAllWindows()
