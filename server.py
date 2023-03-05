from http.server import HTTPServer, BaseHTTPRequestHandler

from io import BytesIO
import cv2 as cv
import numpy as np

from requests_toolbelt.multipart import decoder


class SimpleHTTPRequestHandler(BaseHTTPRequestHandler):

    def do_GET(self):
        self.send_response(200)
        self.end_headers()
        self.wfile.write(b'Hello, world!')

    def do_POST(self):
        self.path = '/upload'
        content_length = int(self.headers['Content-Length'])

        file_content = self.rfile.read(content_length)

        multipart_data = decoder.MultipartDecoder(file_content, self.headers['Content-Type']).parts
        image_byte = multipart_data[0].content
        image_numpy = np.frombuffer(image_byte, np.int8)
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
        response = bytes(im)
        self.send_response(200)
        self.send_header("Content-Length", str(len(response)))
        self.end_headers()
        self.wfile.write(final_im)


httpd = HTTPServer(('localhost', 8001), SimpleHTTPRequestHandler)
httpd.serve_forever()
