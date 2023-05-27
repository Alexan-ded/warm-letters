import copy
import queue
import cv2 as cv


im = cv.imread('lol.jpg')
imgray = cv.cvtColor(im, cv.COLOR_BGR2GRAY)
'''ret, thresh = cv.threshold(imgray, 0, 255, cv.THRESH_OTSU |
                                          cv.THRESH_BINARY_INV) '''

rect_kernel_for_erosion = cv.getStructuringElement(cv.MORPH_RECT, (1, 1))
rect_kernel_for_dilation = cv.getStructuringElement(cv.MORPH_RECT, (18, 1)) # здесь можно менять параметры в зависимости от рамзера текста и разрешения
erosion =  cv.erode(imgray, rect_kernel_for_erosion, iterations = 3)
dilation = cv.dilate(imgray, rect_kernel_for_dilation, iterations = 3)
cv.imwrite("dilation.jpg", dilation)

strs = copy.deepcopy(dilation)
meeted = []
for i in range(len(dilation)):
    meeted.append([])
    for j in range(len(dilation[0])):
        meeted[i].append(0)

count_for_bfs = 100
count_for_img = 1
strings = []
for i in range(len(dilation)):
    for j in range(len(dilation[0])):
        if (int(dilation[i][j]) == 255 and meeted[i][j] == 0):
            first_coord = [i, j]
            second_coord = [i, j]
            count_for_bfs = max(1, (count_for_bfs + 100) % 254) 
            q = queue.Queue()
            q.put((i, j))
            meeted[i][j] = count_for_img;
            strs[i][j] = count_for_bfs # для дебага
            while not q.empty(): 
                cur_i, cur_j = q.get()
                for shift in [[0, -1],[1, 0], [-1, 0], [0, 1]]:
                    new_i = max(0, min(len(dilation) - 1, shift[0] + cur_i))
                    new_j = max(0, min(len(dilation[0]) - 1, shift[1] + cur_j))
                    if (int(dilation[new_i][new_j]) == 255 and meeted[new_i][new_j] == 0):
                        if (first_coord[0] > new_i):
                            first_coord[0] = new_i
                        if (first_coord[1] > new_j):
                            first_coord[1] = new_j
                        if (second_coord[0] < new_i):
                            second_coord[0] = new_i
                        if (second_coord[1] < new_j):
                            second_coord[1] = new_j
                        q.put((new_i, new_j))
                        meeted[new_i][new_j] = count_for_img
                        strs[new_i][new_j] = count_for_bfs
            
            cropped = imgray[first_coord[0]:second_coord[0] + 1, first_coord[1]:second_coord[1] + 1]
            for ii in range(len(cropped)): # в теории можно не чистить изображение
                for jj in range(len(cropped[0])):
                    if (meeted[ii + first_coord[0]][jj + first_coord[1]] != count_for_img):
                        cropped[ii][jj] = 0
                    cropped[ii][jj] = 255 - cropped[ii][jj]
            strings.append(cropped)
            cv.imwrite('img' + str(count_for_img) + '.png', cropped) # здесь и сохраняются картнки отдельных строк
            count_for_img += 1
# strings - массив картинок с отдельными строками
            