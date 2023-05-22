package main

import (
	"image"
	"image/color"
	"os"
	//"fmt"
)

func main () {
	// fmt.Println(os.Args[1])
	pal := color.Palette([]color.Color{
		color.RGBA{0, 0, 0, 0}, // Transparent
		color.RGBA{0, 0, 0, 0xff}, // Black
		color.RGBA{0xff, 0xff, 0xff, 0xff}, // White
	})
	// change this into reading bitmap from file
	var circles [5000]image.Point
	for i := range circles {
		circles[i].X = i
		circles[i].Y = i
	}
	// get from outside
	w, h := 1000, 1000
	// change formula
	pixels_pf := 5 // pixels per frame
	am := BEBR {
		Frames: make([]Frame, 100),
		LoopCount: 1,
	}
	// all pixels counter, must be less or equal then og pixel counter
	// all entries of coun, except ones in last im.Set must be removed 
	coun := 0
	for i := range am.Frames {
		im := image.NewPaletted(image.Rect(coun, coun, coun + pixels_pf, coun + pixels_pf), pal)
		if i == 0 {
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
		am.Frames[i].XOffset = coun
		am.Frames[i].YOffset = coun
		am.Frames[i].DelayNumerator = 1
		am.Frames[i].DelayDenominator = 240

		for j := 0; j < pixels_pf; j++ {
			im.Set(circles[coun].X, circles[coun].Y, pal[1])
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
