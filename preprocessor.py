from scipy.spatial import distance as dist
import numpy as np
import itertools
import math
import cv2


class Scanner(object):
    def __init__(self, interactive=False, min_area_ratio=0.2, max_angle_range=40, min_angle_value=80,
                 max_angle_value=100):
        self.MIN_QUAD_AREA_RATIO = min_area_ratio
        self.MAX_QUAD_ANGLE_RANGE = max_angle_range
        self.MIN_ANGLE_VALUE = min_angle_value
        self.MAX_ANGLE_VALUE = max_angle_value

    def order_points(self, pts):
        x_sort = pts[np.argsort(pts[:, 0]), :]

        leftest = x_sort[:2, :]
        rightest = x_sort[2:, :]

        leftest = leftest[np.argsort(leftest[:, 1]), :]
        (tl, bl) = leftest

        d = dist.cdist(tl[np.newaxis], rightest, "euclidean")[0]
        (br, tr) = rightest[np.argsort(d)[::-1], :]

        return np.array([tl, tr, br, bl], dtype="float32")

    def distance(self, a, b):
        return np.sqrt(((a[0] - b[0]) ** 2) + ((a[1] - b[1]) ** 2))

    def four_point_transform(self, image, pts):
        corners = self.order_points(pts)
        tl, tr, br, bl = corners

        width_bottom = self.distance(br, bl)
        width_top = self.distance(tr, tl)
        new_width = max(int(width_top), int(width_bottom))

        height_right = self.distance(tr, br)
        height_left = self.distance(tl, bl)
        new_height = max(int(height_right), int(height_left))

        dst = np.array([
            [0, 0],
            [new_width - 1, 0],
            [new_width - 1, new_height - 1],
            [0, new_height - 1]], dtype="float32")

        transform_matrix = cv2.getPerspectiveTransform(corners, dst)
        warped = cv2.warpPerspective(image, transform_matrix, (new_width, new_height))

        return warped

    def filter_corners(self, corners, min_dist=20):
        filtered_corners = []
        for c in corners:
            flag = True
            for filtered in filtered_corners:
                if self.distance(filtered, c) < min_dist:
                    flag = False
            if flag is True:
                filtered_corners.append(c)
        return filtered_corners

    def angle_between_vectors(self, u, v):
        radians = math.atan2(u[0] * v[1] - u[1] * v[0], u[0] * v[0] + u[1] * v[1])
        return abs(np.degrees(radians))

    def get_angle(self, p1, p2, p3):
        vec_a = p1 - p2
        vec_b = p3 - p2
        return self.angle_between_vectors(vec_a, vec_b)

    def angle_range(self, quad):
        tl, tr, br, bl = self.order_points(quad.reshape(4, 2))
        ura = self.get_angle(tl, tr, br)
        ula = self.get_angle(bl, tl, tr)
        lra = self.get_angle(tr, br, bl)
        lla = self.get_angle(br, bl, tl)
        angles = [ura, ula, lra, lla]
        return angles

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

    def diagonal_diff(self, cnt):
        ordered_corners = self.order_points(cnt.reshape(4, 2))
        tl, tr, br, bl = ordered_corners
        diag1 = self.distance(tl, br)
        diag2 = self.distance(bl, tr)
        return abs(diag1 - diag2)

    def is_valid_contour(self, cnt, IM_WIDTH, IM_HEIGHT):
        if len(cnt) != 4:
            return False
        if cv2.contourArea(cnt) <= IM_WIDTH * IM_HEIGHT * self.MIN_QUAD_AREA_RATIO:
            return False
        # angles = self.angle_range(cnt)
        # for angle in angles:
        #     if angle <= self.MIN_ANGLE_VALUE or angle >= self.MAX_ANGLE_VALUE:
        #         # print(angles)
        #         # cv2.drawContours(image, [cnt*2], -1, (0, 0, 255), 4)
        #         # cv2.imshow("Outline", image)
        #         # cv2.waitKey(0)
        #         # cv2.destroyAllWindows()
        #         return False
        return True

    def get_contour(self, rescaled_image):
        KERNEL_SIZE = 9
        CANNY = 84

        IM_HEIGHT, IM_WIDTH = rescaled_image.shape

        smooth = cv2.GaussianBlur(rescaled_image, (7, 7), 0)

        kernel = cv2.getStructuringElement(cv2.MORPH_RECT, (KERNEL_SIZE, KERNEL_SIZE))
        dilated = cv2.morphologyEx(smooth, cv2.MORPH_CLOSE, kernel)

        edged = cv2.Canny(dilated, 0, CANNY)
        test_corners = self.get_corners(edged)
        print(test_corners)
        approx_contours = []
        valid_quads = []

        if len(test_corners) >= 4:
            for quad in itertools.combinations(test_corners, 4):
                points = self.order_points(np.array(quad))
                points = np.array([[p] for p in points], dtype="int32")

                cv2.drawContours(rescaled_image, [points], -1, (0, 0, 255), 7)
                print(self.angle_range(points))

                if self.is_valid_contour(points, IM_WIDTH, IM_HEIGHT) is True:
                    valid_quads.append(points)

            valid_quads = sorted(valid_quads, key=cv2.contourArea, reverse=True)[:5]
            # valid_quads = sorted(valid_quads, key=self.diagonal_diff)

            if len(valid_quads) > 0:
                approx_contours = [valid_quads[0]]

        cnts, hierarchy = cv2.findContours(edged.copy(), cv2.RETR_LIST, cv2.CHAIN_APPROX_SIMPLE)
        cnts = sorted(cnts, key=cv2.contourArea, reverse=True)[:5]

        # loop over the contours
        for c in cnts:
            # approximate the contour
            perimeter = cv2.arcLength(c, True)
            approx = cv2.approxPolyDP(c, perimeter * 0.02, True)
            # cv2.drawContours(rescaled_image, [approx], -1, (0, 0, 255), 1)
            if self.is_valid_contour(approx, IM_WIDTH, IM_HEIGHT):
                # cv2.drawContours(rescaled_image, [approx], -1, (0, 0, 255), 5)
                approx_contours.append(approx)
                break

        if not approx_contours:
            TOP_RIGHT = (IM_WIDTH, 0)
            BOTTOM_RIGHT = (IM_WIDTH, IM_HEIGHT)
            BOTTOM_LEFT = (0, IM_HEIGHT)
            TOP_LEFT = (0, 0)
            screenCnt = np.array([[TOP_RIGHT], [BOTTOM_RIGHT], [BOTTOM_LEFT], [TOP_LEFT]])

        else:
            screenCnt = min(approx_contours, key=self.diagonal_diff)

        return screenCnt.reshape(4, 2)

    def scan(self, image):  # изображениe
        # grayscale
        gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)

        # resizing
        ratio = 2  # делаем картинку в два раза меньше
        rescaled_gray = cv2.resize(gray, None, fx=0.5, fy=0.5, interpolation=cv2.INTER_LINEAR)

        # get the contour of the document
        paper_contours = self.get_contour(rescaled_gray)
        transformed = self.four_point_transform(gray, paper_contours * ratio)

        return transformed

#возвращает инверснутое бинарное изображение
def preprocess_img(transformed):  # getting already grayscale
    GAUSS = 95
    KERNEL_SIZE = 3

    # gauss blur
    smooth = cv2.GaussianBlur(transformed, (GAUSS, GAUSS), 0)

    # making shadows brighter
    division = cv2.divide(transformed, smooth, scale=255)

    # sharpening
    # sharp = filters.unsharp_mask(division, radius=1.5, amount=1.5, channel_axis=False, preserve_range=False)
    # sharp = (255 * sharp).clip(0, 255).astype(np.uint8)

    # threshold: инверсим цвета -> текст белый, а фон черный
    thresh = cv2.threshold(division, 0, 255, cv2.THRESH_BINARY_INV + cv2.THRESH_OTSU)[1]

    # dilating
    kernel = cv2.getStructuringElement(cv2.MORPH_CROSS, (KERNEL_SIZE, KERNEL_SIZE))
    # closed = cv2.morphologyEx(thresh, cv2.MORPH_OPEN, kernel)
    dilated = cv2.dilate(thresh, kernel, iterations=1)
    h, w = dilated.shape[:2]
    mask = np.zeros((h + 2, w + 2), np.uint8)
    for x in range(dilated.shape[1]):
        if dilated[0][x] == 255:
            cv2.floodFill(dilated, mask, (x, 0), 0)
        if dilated[dilated.shape[0] - 1][x] == 255:
            cv2.floodFill(dilated, mask, (x, dilated.shape[0] - 1), 0)

    for y in range(dilated.shape[0]):
        if dilated[y][0] == 255:
            cv2.floodFill(dilated, mask, (0, y), 0)
        if dilated[y][dilated.shape[1] - 1] == 255:
            cv2.floodFill(dilated, mask, (dilated.shape[1] - 1, y), 0)

    return dilated


image = cv2.imread("../IMG_3341.jpeg")
scanner = Scanner()
trans = scanner.scan(image)
process = preprocess_img(trans)

cv2.imshow("lol", process)
cv2.waitKey(0)
cv2.destroyAllWindows()
