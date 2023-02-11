import cv2 as cv

im = cv.imread('/Users/danila/Downloads/image.png')
imgray = cv.cvtColor(im, cv.COLOR_BGR2GRAY)
ret, thresh = cv.threshold(imgray, 127, 255, 0)
contours, hierarchy = cv.findContours(thresh, cv.RETR_TREE, cv.CHAIN_APPROX_SIMPLE)

cv.drawContours(im, contours, -1, (0, 255, 0), 3)
cv.imshow("bebra", im)
cv.waitKey(0)
cv.destroyAllWindows()