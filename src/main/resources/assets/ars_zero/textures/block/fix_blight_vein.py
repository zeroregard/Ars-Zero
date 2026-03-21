from PIL import Image
import numpy as np

def clean_alpha_png(input_path, output_path):
    img = Image.open(input_path).convert("RGBA")
    data = np.array(img)

    alpha = data[:, :, 3]

    # Fully transparent pixels
    transparent = alpha == 0

    # Zero out RGB in fully transparent pixels
    data[transparent, :3] = 0

    # Save
    Image.fromarray(data).save(output_path)

# Usage
clean_alpha_png("blight_vein.png", "blight_vein_cleaned.png")