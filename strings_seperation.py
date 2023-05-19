import copy
import queue
import sys
import cv2 as cv

sys.setrecursionlimit(100000)

def dfs(cur_x, cur_y, dilation, strs, meeted, count_for_dfs):
    meeted[cur_x][cur_y] = 10
    strs[cur_x][cur_y] = count_for_dfs
    if (cur_x == 0 or cur_x == len(dilation) - 1 or cur_y == 0 or cur_y == len(dilation[0]) - 1):
        return
    for shift in [[0, -1],[1, 0], [-1, 0], [0, 1]]:
        # print(shift[0], shift[1], int(dilation[shift[0] + cur_x][shift[1] + cur_y]), meeted[shift[0] + cur_x][shift[1] + cur_y])
        if (int(dilation[shift[0] + cur_x][shift[1] + cur_y]) == 255 and meeted[shift[0] + cur_x][shift[1] + cur_y] == 0):
            # print(shift[0], shift[1], cur_x, cur_y)
            dfs(shift[0] + cur_x, shift[1] + cur_y, dilation, strs, meeted, count_for_dfs)

    

im = cv.imread('img.jpg')
imgray = cv.cvtColor(im, cv.COLOR_BGR2GRAY)
ret, thresh = cv.threshold(imgray, 0, 255, cv.THRESH_OTSU |
                                          cv.THRESH_BINARY_INV) # поэкспериметруй с параметрами
cv.imwrite('hz.jpg', thresh)

rect_kernel_for_erosion = cv.getStructuringElement(cv.MORPH_RECT, (1, 1))
rect_kernel_for_dilation = cv.getStructuringElement(cv.MORPH_RECT, (15, 1))
erosion =  cv.erode(thresh, rect_kernel_for_erosion, iterations = 3)
cv.imwrite('erosition_image.jpg', erosion)
# dilation = cv.dilate(erosion, rect_kernel_for_dilation, iterations = 3)


dilation = cv.dilate(thresh, rect_kernel_for_dilation, iterations = 3)
# dilation = dilation[0:100, 0:100]
cv.imwrite('dilation_image.jpg',dilation)
strs = copy.deepcopy(dilation)
meeted = []
for i in range(len(dilation)):
    meeted.append([])
    for j in range(len(dilation[0])):
        meeted[i].append(0)
print('ok')
count_for_bfs = 100;
for i in range(len(dilation)):
    for j in range(len(dilation[0])):
        if (int(dilation[i][j]) == 255 and meeted[i][j] == 0):
            first_coord = [i, j]
            second_coord = [i, j]
            count_for_bfs = max(1, (count_for_bfs + 100) % 254) 
            q = queue.Queue()
            q.put((i, j))
            meeted[i][j] = 1;
            strs[i][j] = count_for_bfs
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
                        meeted[new_i][new_j] = 1
                        strs[new_i][new_j] = count_for_bfs
            print(first_coord[0], second_coord[0], first_coord[1],second_coord[1])
            cropped = im[first_coord[0]:second_coord[0] + 1, first_coord[1]:second_coord[1] + 1] # +1 лишний
            print(len(cropped), len(cropped[0]))
            cv.imwrite('beb' + str(i) + '.jpg', cropped)

            
print('ok');
cv.imwrite('mol.jpg', strs)
contours, hierarchy = cv.findContours(dilation, cv.RETR_LIST, cv.CHAIN_APPROX_TC89_KCOS)
count  = -1;
for cnt in contours:
    count += 1
    x, y, w, h = cv.boundingRect(cnt)
     
    # Draw the bounding box on the text area
    rect=cv.rectangle(im, (x, y), (x + w, y + h), (0, 255, 0), 2)
     
    # Crop the bounding box area
    cropped = im[y:y + h, x:x + w]
    
    cv.imwrite('rectanglebox' + str(count) + '.jpg', cropped)

cv.drawContours(im, contours, -1, (0, 255, 0), 3)
cv.imwrite("bebra.jpg", im)
