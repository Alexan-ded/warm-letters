from scipy.spatial import distance as dist
import numpy as np
import itertools
import math
import cv2


class Scanner(object):
    def __init__(self, interactive=False, MIN_QUAD_AREA_RATIO=0.2, MAX_QUAD_ANGLE_RANGE=40):
        self.MIN_QUAD_AREA_RATIO = MIN_QUAD_AREA_RATIO
        self.MAX_QUAD_ANGLE_RANGE = MAX_QUAD_ANGLE_RANGE

    def order_points(self, pts):
        xSorted = pts[np.argsort(pts[:, 0]), :]
        leftMost = xSorted[:2, :]
        rightMost = xSorted[2:, :]
        leftMost = leftMost[np.argsort(leftMost[:, 1]), :]
        (tl, bl) = leftMost
        D = dist.cdist(tl[np.newaxis], rightMost, "euclidean")[0]
        (br, tr) = rightMost[np.argsort(D)[::-1], :]
        return np.array([tl, tr, br, bl], dtype="float32")

    def filter_corners(self, corners, min_dist=20):
        filtered_corners = []
        for c in corners:
            flag = True
            for filtered in filtered_corners:
                if dist.euclidean(filtered, c) < min_dist:
                    flag = False
            if flag is True:
                filtered_corners.append(c)
        return filtered_corners

    def angle_between_vectors(self, u, v):
        radians = math.atan2(u[0] * v[1] - u[1] * v[0], u[0] * v[0] + u[1] * v[1])
        return abs(np.degrees(radians))

    def get_angle(self, p1, p2, p3):
        a = np.array(p1)
        b = np.array(p2)
        c = np.array(p3)

        avec = a - b
        cvec = c - b
        return self.angle_between_vectors(avec, cvec)

    def angle_range(self, quad):
        tl, tr, br, bl = quad
        ura = self.get_angle(tl[0], tr[0], br[0])
        ula = self.get_angle(bl[0], tl[0], tr[0])
        lra = self.get_angle(tr[0], br[0], bl[0])
        lla = self.get_angle(br[0], bl[0], tl[0])
        angles = [ura, ula, lra, lla]
        return np.ptp(angles)

    def get_corners(self, img):
        line_detector = cv2.createLineSegmentDetector(0)
        lines = line_detector.detect(img)
        corners = []
        if lines is not None:
            horizontal_lines_canvas = np.zeros(img.shape, dtype=np.uint8)
            vertical_lines_canvas = np.zeros(img.shape, dtype=np.uint8)
            for line in lines[0]:
                line = line.squeeze().astype(np.int32).tolist()
                x1, y1, x2, y2 = line
                if abs(x2 - x1) > abs(y2 - y1):  # horizontal
                    if x1 > x2:
                        x1, x2 = x2, x1
                        y1, y2 = y2, y1
                    cv2.line(horizontal_lines_canvas, (max(x1 - 5, 0), y1), (min(x2 + 5, img.shape[1] - 1), y2), 255, 2)
                else:  # vertical
                    if y1 > y2:
                        y1, y2 = y2, y1
                        x1, x2 = x2, x1
                    cv2.line(vertical_lines_canvas, (x1, max(y1 - 5, 0)), (x2, min(y2 + 5, img.shape[0] - 1)), 255, 2)

            contours, hierarchy = cv2.findContours(horizontal_lines_canvas, cv2.RETR_EXTERNAL,
                                                   cv2.CHAIN_APPROX_NONE)  # TODO
            contours = sorted(contours, key=lambda c: cv2.arcLength(c, True), reverse=True)[:2]
            horizontal_lines_canvas = np.zeros(img.shape, dtype=np.uint8)
            for contour in contours:
                contour = np.squeeze(contour)
                min_x = min(contour[:, 0]) + 2
                max_x = max(contour[:, 0]) - 2
                y_for_min_x = 0
                y_for_max_x = 0
                cnt1 = 0
                cnt2 = 0
                for dot in contour:
                    if dot[0] == min_x:
                        y_for_min_x += dot[1]
                        cnt1 += 1
                    if dot[0] == max_x:
                        y_for_max_x += dot[1]
                        cnt2 += 1
                y_for_min_x /= cnt1
                y_for_min_x = int(y_for_min_x)

                y_for_max_x /= cnt2
                y_for_max_x = int(y_for_max_x)

                cv2.line(horizontal_lines_canvas, (min_x, y_for_min_x), (max_x, y_for_max_x), 1, 1)
                corners.append((min_x, y_for_min_x))
                corners.append((max_x, y_for_max_x))

            contours, hierarchy = cv2.findContours(vertical_lines_canvas, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_NONE)
            contours = sorted(contours, key=lambda c: cv2.arcLength(c, True), reverse=True)[:2]
            vertical_lines_canvas = np.zeros(img.shape, dtype=np.uint8)
            for contour in contours:
                contour = np.squeeze(contour)
                min_y = min(contour[:, 1]) + 2
                max_y = max(contour[:, 1]) - 2
                x_for_min_y = 0
                x_for_max_y = 0
                cnt1 = 0
                cnt2 = 0
                for dot in contour:
                    if dot[1] == min_y:
                        x_for_min_y += dot[0]
                        cnt1 += 1
                    if dot[1] == max_y:
                        x_for_max_y += dot[0]
                        cnt2 += 1
                x_for_min_y /= cnt1
                x_for_min_y = int(x_for_min_y)
                x_for_max_y /= cnt2
                x_for_max_y = int(x_for_max_y)

                cv2.line(vertical_lines_canvas, (x_for_min_y, min_y), (x_for_max_y, max_y), 1, 1)
                corners.append((x_for_min_y, min_y))
                corners.append((x_for_max_y, max_y))
            corners_y, corners_x = np.where(horizontal_lines_canvas + vertical_lines_canvas == 2)
            corners += zip(corners_x, corners_y)

        corners = self.filter_corners(corners)
        return corners

    def is_valid_contour(self, cnt, IM_WIDTH, IM_HEIGHT):
        return (len(cnt) == 4 and cv2.contourArea(cnt) > IM_WIDTH * IM_HEIGHT * self.MIN_QUAD_AREA_RATIO
                and self.angle_range(cnt) < self.MAX_QUAD_ANGLE_RANGE)

    def get_contour(self, rescaled_image):
        MORPH = 9
        CANNY = 84
        HOUGH = 25

        IM_HEIGHT, IM_WIDTH, _ = rescaled_image.shape

        gray = cv2.cvtColor(rescaled_image, cv2.COLOR_BGR2GRAY)
        gray = cv2.GaussianBlur(gray, (7, 7), 0)

        kernel = cv2.getStructuringElement(cv2.MORPH_RECT, (MORPH, MORPH))
        dilated = cv2.morphologyEx(gray, cv2.MORPH_CLOSE, kernel)

        edged = cv2.Canny(dilated, 0, CANNY)
        test_corners = self.get_corners(edged)
        approx_contours = []
        if len(test_corners) >= 4:
            quads = []
            for quad in itertools.combinations(test_corners, 4):
                points = self.order_points(np.array(quad))
                points = np.array([[p] for p in points], dtype="int32")
                quads.append(points)

            quads = sorted(quads, key=cv2.contourArea, reverse=True)[:5]
            quads = sorted(quads, key=self.angle_range)
            approx = quads[0]
            if self.is_valid_contour(approx, IM_WIDTH, IM_HEIGHT):
                approx_contours.append(approx)

        cnts, hierarchy = cv2.findContours(edged.copy(), cv2.RETR_LIST, cv2.CHAIN_APPROX_SIMPLE)
        cnts = sorted(cnts, key=cv2.contourArea, reverse=True)[:5]

        for c in cnts:
            perimeter = cv2.arcLength(c, True)
            approx = cv2.approxPolyDP(c, perimeter * 0.02, True)
            if self.is_valid_contour(approx, IM_WIDTH, IM_HEIGHT):
                approx_contours.append(approx)
                break

        if not approx_contours:
            TOP_RIGHT = (IM_WIDTH, 0)
            BOTTOM_RIGHT = (IM_WIDTH, IM_HEIGHT)
            BOTTOM_LEFT = (0, IM_HEIGHT)
            TOP_LEFT = (0, 0)
            screenCnt = np.array([[TOP_RIGHT], [BOTTOM_RIGHT], [BOTTOM_LEFT], [TOP_LEFT]])

        else:
            screenCnt = max(approx_contours, key=cv2.contourArea)

        return screenCnt.reshape(4, 2)

    def scan(self, path):
        image = cv2.imread(path)
        rescaled_image = cv2.resize(image, None, fx=0.5, fy=0.5, interpolation=cv2.INTER_LINEAR)
        screenCnt = self.get_contour(rescaled_image)
        cv2.drawContours(rescaled_image, [screenCnt], -1, (0, 255, 0), 3)
        cv2.imshow("Outline", rescaled_image)
        cv2.waitKey(0)
        cv2.destroyAllWindows()


scanner = Scanner()
scanner.scan("../IMG_3341.jpeg")  # путь к картинке сюда
