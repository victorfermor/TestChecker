import json
import random
import uuid
import os
import qrcode
from fpdf import FPDF
from PIL import Image

# ------------- Configuración ------------------

NUM_EXAMENES = 1
PREGUNTAS_POR_EXAMEN = 10
CARPETA_SALIDA = "examenes"

# Cargar preguntas
preguntas = []

with open("preguntas.json", "r", encoding="utf-8") as f:
    preguntas = json.load(f)

print(f"Se han cargado {len(preguntas)} preguntas desde preguntas.json")

os.makedirs(CARPETA_SALIDA, exist_ok=True)

# ------------- Funciones ------------------

def generar_qr(data_dict, filepath):
    data_json = json.dumps(data_dict)
    img = qrcode.make(data_json)
    img.save(filepath)

def crear_pdf_examen(examen_id, preguntas_examen, respuestas_correctas, qr_path, output_path, titulo="Título del Examen"):
    pdf = FPDF()
    pdf.add_page()
    pdf.set_font("Arial", size=12)

    page_w = 210  # A4 width in mm

    # ---- Cabecera: Examen ID y Título centrados ----
    pdf.set_font("Arial", "", 9)
    pdf.cell(0, 10, f"EXAMEN ID: {examen_id}", ln=True, align='C')

    pdf.ln(5)

    pdf.set_font("Arial", "B", 20)
    pdf.cell(0, 10, titulo, ln=True, align='C')

    pdf.ln(5)

    # ---- Cuadro para nombre y fecha ----
    cuadro_w = 100
    cuadro_x = (page_w - cuadro_w) / 2
    cuadro_y = pdf.get_y()

    pdf.set_xy(cuadro_x, cuadro_y)
    pdf.set_font("Arial", size=11)
    pdf.cell(cuadro_w, 8, "Nombre: __________________________________________", ln=True)
    pdf.set_x(cuadro_x)
    pdf.cell(cuadro_w, 8, "Fecha:   __________________________________________", ln=True)

    pdf.ln(25)

    # ---- Zona central de detección ----
    block_w = 170

    # Calcular altura según número de preguntas
    cell_spacing_y = 12
    num_preguntas = len(preguntas_examen)
    qr_size = 60
    margen_superior = 10
    margen_inferior = 10
    titulo_y = 10  # espacio para letras A B C D
    required_grid_height = (num_preguntas + 1) * cell_spacing_y + titulo_y 
    block_h = max(qr_size + margen_superior + margen_inferior, required_grid_height + margen_superior + margen_inferior)

    block_x = (page_w - block_w) / 2
    block_y = pdf.get_y()

    # Medidas QR
    qr_size = 60
    qr_x = block_x + 5
    qr_y = block_y + 5
    pdf.image(qr_path, x=qr_x, y=qr_y, w=qr_size, h=qr_size)

    # Cuadrícula
    start_x = qr_x + qr_size + 10
    start_y = qr_y
    cell_spacing_x = 18
    cell_spacing_y = 12
    radio = 5
    opciones = ["A", "B", "C", "D"]

    # Títulos A B C D
    pdf.set_xy(start_x + 15, start_y)
    pdf.set_font("Arial", "B", 11)
    for i, op in enumerate(opciones):
        pdf.set_x(start_x + 20 + i * cell_spacing_x)
        pdf.cell(10, 10, op, ln=0)

    # Círculos
    pdf.set_font("Arial", size=11)
    for i in range(len(preguntas_examen)):
        y = start_y + (i + 1) * cell_spacing_y + 2
        pdf.set_xy(start_x, y)
        pdf.cell(10, 8, f"{i + 1}:", ln=0)
        for j in range(4):
            x = start_x + 20 + j * cell_spacing_x
            pdf.ellipse(x, y, 2 * radio, 2 * radio)

    # ---- Marcas de esquina para detección ----
    mark_size = 8
    pdf.set_fill_color(0)
    # Top-left
    pdf.rect(block_x - mark_size, block_y - mark_size, mark_size, mark_size, 'F')
    # Top-right
    pdf.rect(block_x + block_w, block_y - mark_size, mark_size, mark_size, 'F')
    # Bottom-left
    pdf.rect(block_x - mark_size, block_y + block_h, mark_size, mark_size, 'F')
    # Bottom-right
    pdf.rect(block_x + block_w, block_y + block_h, mark_size, mark_size, 'F')

    # ---- Página 2: preguntas completas ----
    pdf.add_page()
    pdf.set_xy(10, 20)
    pdf.set_font("Arial", size=12)
    for idx, pregunta in enumerate(preguntas_examen):
        pdf.multi_cell(0, 10, f"{idx + 1}. {pregunta['pregunta']}")
        for i, opcion in enumerate(pregunta["opciones"]):
            pdf.set_x(15)
            pdf.cell(0, 10, f"({chr(65 + i)}) {opcion}", ln=True)
        pdf.ln(1)

    pdf.output(output_path)



# ------------- Generación ------------------

for i in range(NUM_EXAMENES):
    examen_id = str(uuid.uuid4())
    preguntas_examen = random.sample(preguntas, PREGUNTAS_POR_EXAMEN)
    respuestas_correctas = {str(idx + 1): q["respuesta_correcta"] for idx, q in enumerate(preguntas_examen)}

    qr_data = {
        "id": examen_id,
        "respuestas_correctas": respuestas_correctas
    }

    qr_filename = os.path.join(CARPETA_SALIDA, f"qr_examen_{i+1}.png")
    generar_qr(qr_data, qr_filename)

    pdf_filename = os.path.join(CARPETA_SALIDA, f"examen_{i+1}.pdf")
    crear_pdf_examen(examen_id, preguntas_examen, respuestas_correctas, qr_filename, pdf_filename)

    print(f"✅ Examen {i+1} generado: {pdf_filename}")


