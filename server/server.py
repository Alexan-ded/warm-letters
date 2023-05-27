from http.server import HTTPServer, BaseHTTPRequestHandler
from io import BytesIO
from preprocessor import Scanner, preprocess_img
from strings_seperation import separator
from requests_toolbelt.multipart import decoder
from time import sleep

import cv2 as cv
import numpy as np
import yaml
import subprocess


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
            multipart_data = decoder.MultipartDecoder(file_content, content_type).parts
            image_bytes = multipart_data[0].content
        elif 'image/jpeg' in content_type:
            image_bytes = file_content
        else:
            print("Error: content_type is not recognized")
            exit()
        
        image_numpy = np.frombuffer(image_bytes, np.int8)
        im = cv.imdecode(image_numpy, cv.IMREAD_UNCHANGED)
        scanner = Scanner()
        inp = scanner.scan(im) # result from first block
        out = preprocess_img(inp)
        img_counter = separator(out) # result from second block

        jar_path = './java_exec.jar'
        process = subprocess.Popen(['java', '-jar', jar_path, str(img_counter)], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        stdout, stderr = process.communicate()
        output = stdout.decode('utf-8')
        number_list = [int(num) for num in output.split()]
        point_list = [number_list[i:i+2] for i in range(0, len(number_list), 2)]
        print(point_list[:100])
        
        self.send_response(200)
        self.end_headers()
        print('Success')

from os.path import abspath
config_path = '../app/src/main/assets/config/config.yaml'
try:
    with open(config_path) as f:
        config = yaml.load(f, Loader=yaml.FullLoader)
except FileNotFoundError:
    print(f"Error: config file {abspath(config_path)} not found.\nPlease add a config file")
    exit()

ip_address = config['ip_address']
port = int(config['port'])

httpd = HTTPServer((ip_address, port), SimpleHTTPRequestHandler)
print(f"Server running at http://{ip_address}:{port}/")

httpd.serve_forever()
