package main

import (
	"io"
	"image"
	"image/color"
)

// Encode writes the BEBR a to w in PNG format.
func Encode(w io.Writer, a BEBR) error {
	var e Encoder
	return e.Encode(w, a)
}

// Encode writes the Animation a to w in PNG format.
func (enc *Encoder) Encode(w io.Writer, a BEBR) error {
	var e *encoder
	if enc.BufferPool != nil {
		buffer := enc.BufferPool.Get()
		e = (*encoder)(buffer)

	}
	if e == nil {
		e = &encoder{}
	}
	if enc.BufferPool != nil {
		defer enc.BufferPool.Put((*EncoderBuffer)(e))
	}

	e.enc = enc
	e.writer = w
	e.a = a

	var pal color.Palette

	if _, ok := a.Frames[0].Image.(image.PalettedImage); ok {
		pal, _ = a.Frames[0].Image.ColorModel().(color.Palette)
	}
	if len(pal) <= 2 {
		e.cb = cbP1
	} else if len(pal) <= 4 {
		e.cb = cbP2
	}

	_, e.err = io.WriteString(w, pngHeader)
	e.writeIHDR()
	e.writePLTEAndTRNS(pal)
	if len(e.a.Frames) > 1 {
		e.writeacTL()
	}
	if !e.a.Frames[0].IsDefault {
		e.writefcTL(e.a.Frames[0])
	}
	e.writeIDATs()
	for i := 0; i < len(e.a.Frames); i = i + 1 {
		if i != 0 && !e.a.Frames[i].IsDefault {
			e.writefcTL(e.a.Frames[i])
			e.writefdATs(e.a.Frames[i])
		}
	}
	e.writeChunk(nil, "IEND")
	return e.err
}
