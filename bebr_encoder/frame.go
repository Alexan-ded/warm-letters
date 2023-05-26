package main

import (
	"image"
)

type BEBR struct {
	Frames []Frame
	LoopCount uint
}

type Frame struct {
	Image            image.Image
	width, height    int
	XOffset, YOffset int
	DelayNumerator   uint16
	DelayDenominator uint16
	DisposeOp        byte
	BlendOp          byte
	IsDefault bool
}
