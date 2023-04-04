from http.server import HTTPServer, BaseHTTPRequestHandler

from io import BytesIO
import cv2 as cv
import numpy as np
import yaml

from requests_toolbelt.multipart import decoder

from time import sleep


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

        # our machine start
        imgray = cv.cvtColor(im, cv.COLOR_BGR2GRAY)
        ret, thresh = cv.threshold(imgray, 127, 255, 0)
        contours, hierarchy = cv.findContours(thresh, cv.RETR_TREE, cv.CHAIN_APPROX_SIMPLE)

        cv.drawContours(im, contours, -1, (0, 255, 0), 3)
        #cv.imshow("bebra", im)
        #cv.waitKey(0)
        #cv.destroyAllWindows()
        # our machine end
        final_im = cv.imencode('.jpg', im)[1].tobytes()

        with open("image.jpg", "wb") as f:
            f.write(final_im)

        sleep(11)
        
        self.send_response(200)
        self.end_headers()
        print('Success')


with open('../Camera4/app/src/main/assets/config/config.yaml') as f:
    config = yaml.load(f, Loader=yaml.FullLoader)
ip_address = config['ip_address']
port = int(config['port'])

httpd = HTTPServer((ip_address, port), SimpleHTTPRequestHandler)
print(f"Server running at http://{ip_address}:{port}/")

httpd.serve_forever()
