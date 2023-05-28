import cv2 as cv
import numpy as np
import yaml
import subprocess
import ctypes
import platform

from http.server import HTTPServer, BaseHTTPRequestHandler
from io import BytesIO
from preprocessor import Scanner, preprocess_img
from strings_seperation import separator
from requests_toolbelt.multipart import decoder
from time import sleep
from pathlib import Path


class SimpleHTTPRequestHandler(BaseHTTPRequestHandler):

    def do_GET(self):
        self.send_response(200)
        self.end_headers()
        self.wfile.write(b'Hello, world!')

    def do_POST(self):
        self.path = '/upload'

        content_type = self.headers['Content-Type']
        content_length = int(self.headers['Content-Length'])

        file_content = self.rfile.read(content_length)

        image_bytes = file_content

        if 'multipart/form-data' in content_type:
            multipart_data = decoder.MultipartDecoder(file_content,
                                                      content_type).parts
            image_bytes = multipart_data[0].content
        elif 'image/jpeg' in content_type:
            image_bytes = file_content
        else:
            print("Error: content_type is not recognized")
            exit()

        image_numpy = np.frombuffer(image_bytes, np.int8)
        im = cv.imdecode(image_numpy, cv.IMREAD_UNCHANGED)
        scanner = Scanner()
        inp = scanner.scan(im)  # result from first block
        
        Path("./photos").mkdir(exist_ok=True)
        out = preprocess_img(inp)
        lines_pos = separator(out) # result from second block

        height, width = out.shape[:2]
        jar_path = './stroke_extraction/target/stroke_extraction.jar'
        params = [str(elem) for elem in lines_pos]
        process = subprocess.Popen(['java', '-jar', jar_path] + params,
                                   stdout=subprocess.PIPE,
                                   stderr=subprocess.PIPE)
        stdout, stderr = process.communicate()
        number_list = [int(num) for num in stdout.split()]
        point_list = [number_list[i:i + 2] for i in range(0, len(number_list), 2)]
        
        if platform.system() == "Windows":
            lib = ctypes.CDLL('../bebr_encoder/main.dll')
        else:
            lib = ctypes.CDLL('../bebr_encoder/main.so')

        lib.file_creator.argtypes = (ctypes.POINTER(ctypes.c_int),
                                      ctypes.c_int, ctypes.c_int, ctypes.c_int)
        c_point_list = (ctypes.c_int * len(number_list))(*number_list)
        lib.file_creator(c_point_list, len(number_list), height, width)

        self.send_response(200)
        self.end_headers()
        print('Success')


from os.path import abspath

config_path = '../app/src/main/assets/config/config.yaml'
try:
    with open(config_path) as f:
        config = yaml.load(f, Loader=yaml.FullLoader)
except FileNotFoundError:
    print(
        f"Error: config file {abspath(config_path)} not found.\nPlease add a config file")
    exit()

ip_address = config['ip_address']
port = int(config['port'])

httpd = HTTPServer((ip_address, port), SimpleHTTPRequestHandler)
print(f"Server running at http://{ip_address}:{port}/")

httpd.serve_forever()
