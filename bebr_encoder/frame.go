package main

import (
	"image"
)

type BEBR struct {
	Frames []Frame
	// LoopCount defines the number of times an animation will be
	// restarted during display.
	// A LoopCount of 0 means to loop forever
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
	IsDefault bool // is image first
}
