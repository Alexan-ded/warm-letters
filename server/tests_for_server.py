import requests
from http.server import HTTPServer, BaseHTTPRequestHandler

from io import BytesIO
import cv2 as cv
import numpy as np

from requests_toolbelt.multipart import decoder

import cv2 as cv
file = open('/Users/danila/Downloads/p1_30509135221673.jpg', 'rb')
#print(file)
output = requests.post("http://127.0.0.1:8001/upload", files={'image': file})

image_byte = output.content
image_numpy = np.frombuffer(image_byte, np.int8)
im = cv.imdecode(image_numpy, cv.IMREAD_UNCHANGED)
cv.imshow("bebra", im)
cv.waitKey(0)
cv.destroyAllWindows()
