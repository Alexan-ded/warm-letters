package main

import "C"
import (
	"image"
	"image/color"
	"os"
	"unsafe"
)

func minandmax(values []image.Point) (int, int, int, int) {
	minX := values[0].X
	maxX := values[0].X 
	minY := values[0].Y
	maxY := values[0].Y
	for _, number := range values {
		if number.X < minX {
			minX = number.X
		}
		if number.X > maxX {
			maxX = number.X
		}
		if number.Y < minY {
			minY = number.Y
		}
		if number.Y > maxY {
			maxY = number.Y
		}
	}
	return minX, maxX, minY, maxY
}

//export file_creator
func file_creator(numbers *C.int, length C.int, height C.int, width C.int) {
    go_numbers := (*[1 << 30]C.int)(unsafe.Pointer(numbers))[:length:length]
	go_length := int(length)
	pal := color.Palette([]color.Color{
		color.RGBA{0, 0, 0, 0}, // Transparent
		color.RGBA{0, 0, 0, 0xff}, // Black
		color.RGBA{0xff, 0xff, 0xff, 0xff}, // White
	})

	circles := make([]image.Point, go_length / 2)
	for i := range circles {
		circles[i].X = int(go_numbers[2 * i])
		circles[i].Y = int(go_numbers[2 * i + 1])
	}
	h, w := int(height), int(width)
	// change formula
	pixels_pf := 20 // pixels per frame
	am := BEBR {
		Frames: make([]Frame, go_length / (2 * pixels_pf)),
		LoopCount: 1,
	}

	coun := 0
	for i := range am.Frames {
		minX, maxX, minY, maxY := minandmax(circles[coun:coun+pixels_pf])
		if (coun > go_length / 2) {
			break
		}
		Xoffset, Yoffset := minX, minY
		im := image.NewPaletted(image.Rect(0, 0, maxX - minX + 1, maxY - minY + 1), pal)
		if i == 0 {
			Xoffset, Yoffset = 0, 0
			im = image.NewPaletted(image.Rect(0, 0, w, h), pal)
			for x := 0; x < w; x++ {
				for y := 0; y < h; y++ {
					im.Set(x, y, pal[2])
				}
			}
		}

		am.Frames[i].Image = im
		am.Frames[i].DisposeOp = 0
		am.Frames[i].BlendOp = 1
		am.Frames[i].XOffset = Xoffset
		am.Frames[i].YOffset = Yoffset
		am.Frames[i].DelayNumerator = 1
		am.Frames[i].DelayDenominator = 240

		for j := 0; j < pixels_pf; j++ {
			im.Set(circles[coun].X - Xoffset, circles[coun].Y - Yoffset, pal[1])
			coun++
		}
	}

	f, err := os.Create("rgb.png")
	if err != nil {
		panic(err)
	}
	defer f.Close()

	if err := Encode(f, am); err != nil {
		panic(err)
	}
}

func main() {
	// go_length := 100

	// pal := color.Palette([]color.Color{
	// 	color.RGBA{0, 0, 0, 0}, // Transparent
	// 	color.RGBA{0, 0, 0, 0xff}, // Black
	// 	color.RGBA{0xff, 0xff, 0xff, 0xff}, // White
	// })
	// circles := make([]image.Point, go_length / 2)
	// for i := range circles {
	// 	circles[i].X = i
	// 	circles[i].Y = i
	// }
	// h, w := 1000, 800
	// // change formula
	// pixels_pf := 5 // pixels per frame
	// am := BEBR {
	// 	Frames: make([]Frame, go_length / (2 * pixels_pf)),
	// 	LoopCount: 1,
	// }

	// coun := 0	
	// for i := range am.Frames {
	// 	minX, maxX, minY, maxY := minandmax(circles[coun:coun+pixels_pf])
	// 	if (coun > go_length / 2) {
	// 		break
	// 	}
	// 	Xoffset, Yoffset := minX, minY
	// 	im := image.NewPaletted(image.Rect(minX, minY, maxX + 1, maxY + 1), pal)
	// 	if i == 0 {
	// 		Xoffset, Yoffset = 0, 0
	// 		im = image.NewPaletted(image.Rect(0, 0, w, h), pal)
	// 		for x := 0; x < w; x++ {
	// 			for y := 0; y < h; y++ {
	// 				im.Set(x, y, pal[2])
	// 			}
	// 		}
	// 	}
	// 	am.Frames[i].Image = im
  	// 	am.Frames[i].DisposeOp = 0
  	// 	am.Frames[i].BlendOp = 1
  	// 	am.Frames[i].XOffset = Xoffset
  	// 	am.Frames[i].YOffset = Yoffset
  	// 	am.Frames[i].DelayNumerator = 1
  	// 	am.Frames[i].DelayDenominator = 240
	// 	for j := 0; j < pixels_pf; j++ {
	// 		im.Set(circles[coun].X, circles[coun].Y, pal[1])
	// 		coun++
	// 	}
	// }
	// f, err := os.Create("rgb.png")
	// if err != nil {
	// 	panic(err)
	// }
	// defer f.Close()

	// if err := Encode(f, am); err != nil {
	// 	panic(err)
	// }
}
