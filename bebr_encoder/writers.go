package main

import (
	"bufio"
	"compress/zlib"
	"encoding/binary"
	"hash/crc32"
	"image"
	"image/color"
	"io"
)

func (e *encoder) writeChunk(b []byte, name string) {
	if e.err != nil {
		return
	}
	n := uint32(len(b))
	binary.BigEndian.PutUint32(e.header[:4], n)
	e.header[4] = name[0]
	e.header[5] = name[1]
	e.header[6] = name[2]
	e.header[7] = name[3]
	crc := crc32.NewIEEE()
	crc.Write(e.header[4:8])
	crc.Write(b)
	binary.BigEndian.PutUint32(e.footer[:4], crc.Sum32())

	_, e.err = e.writer.Write(e.header[:8])
	if e.err != nil {
		return
	}
	_, e.err = e.writer.Write(b)
	if e.err != nil {
		return
	}
	_, e.err = e.writer.Write(e.footer[:4])
}

func (e *encoder) writeIHDR() {
	b := e.a.Frames[0].Image.Bounds()
	binary.BigEndian.PutUint32(e.tmp[0:4], uint32(b.Dx()))
	binary.BigEndian.PutUint32(e.tmp[4:8], uint32(b.Dy()))
	// Set bit depth and color type.
	switch e.cb {
	case cbP2:
		e.tmp[8] = 2
		e.tmp[9] = 3
	case cbP1:
		e.tmp[8] = 1
		e.tmp[9] = 3
	}
	e.tmp[10] = 0
	e.tmp[11] = 0
	e.tmp[12] = 0
	e.writeChunk(e.tmp[:13], "IHDR")
}

func (e *encoder) writeacTL() {
	binary.BigEndian.PutUint32(e.tmp[0:4], uint32(len(e.a.Frames)))
	binary.BigEndian.PutUint32(e.tmp[4:8], uint32(e.a.LoopCount))
	e.writeChunk(e.tmp[:8], "acTL")
}

func (e *encoder) writefcTL(f Frame) {
	binary.BigEndian.PutUint32(e.tmp[0:4], uint32(e.seq))
	e.seq = e.seq + 1
	b := f.Image.Bounds()
	binary.BigEndian.PutUint32(e.tmp[4:8], uint32(b.Dx()))
	binary.BigEndian.PutUint32(e.tmp[8:12], uint32(b.Dy()))
	binary.BigEndian.PutUint32(e.tmp[12:16], uint32(f.XOffset))
	binary.BigEndian.PutUint32(e.tmp[16:20], uint32(f.YOffset))
	binary.BigEndian.PutUint16(e.tmp[20:22], f.DelayNumerator)
	binary.BigEndian.PutUint16(e.tmp[22:24], f.DelayDenominator)
	e.tmp[24] = f.DisposeOp
	e.tmp[25] = f.BlendOp
	e.writeChunk(e.tmp[:26], "fcTL")
}

func (e *encoder) writefdATs(f Frame) {
	e.writeType = 1
	if e.err != nil {
		return
	}
	if e.bw == nil {
		e.bw = bufio.NewWriterSize(e, 1<<15)
	} else {
		e.bw.Reset(e)
	}
	e.err = e.writeImage(e.bw, f.Image, e.cb, e.enc.CompressionLevel)
	if e.err != nil {
		return
	}
	e.err = e.bw.Flush()
}

func (e *encoder) writePLTEAndTRNS(p color.Palette) {
	last := -1
	for i, c := range p {
		c1 := color.NRGBAModel.Convert(c).(color.NRGBA)
		e.tmp[3*i + 0] = c1.R
		e.tmp[3*i + 1] = c1.G
		e.tmp[3*i + 2] = c1.B
		if c1.A != 0xff {
			last = i
		}
		e.tmp[3*256 + i] = c1.A
	}
	e.writeChunk(e.tmp[:3*len(p)], "PLTE")
	if last != -1 {
		e.writeChunk(e.tmp[3*256:3*256 + 1 + last], "tRNS")
	}
}

// An encoder is an io.Writer that satisfies writes by writing PNG IDAT chunks,
// including an 8-byte header and 4-byte CRC checksum per Write call. Such calls
// should be relatively infrequent, since writeIDATs uses a bufio.Writer.
//
// This method should only be called from writeIDATs (via writeImage).
// No other code should treat an encoder as an io.Writer.
func (e *encoder) Write(b []byte) (int, error) {
	if e.writeType == 0 {
		e.writeChunk(b, "IDAT")
	} else {
		c := make([]byte, 4)
		binary.BigEndian.PutUint32(c[0:4], uint32(e.seq))
		e.seq = e.seq + 1
		b = append(c, b...)
		e.writeChunk(b, "fdAT")
	}
	if e.err != nil {
		return 0, e.err
	}
	return len(b), nil
}


func zeroMemory(v []uint8) {
	for i := range v {
		v[i] = 0
	}
}

func (e *encoder) writeImage(w io.Writer, m image.Image, cb int, level CompressionLevel) error {
	if e.zw == nil || e.zwLevel != level {
		if e.enc.CompressionWriter != nil {
			zw, err := e.enc.CompressionWriter(w)
			if err != nil {
				return err
			}
			e.zw = zw
		} else {
			zw, err := zlib.NewWriterLevel(w, levelToZlib(level))
			if err != nil {
				return err
			}
			e.zw = zw
		}
		e.zwLevel = level
	} else {
		e.zw.Reset(w)
	}
	defer e.zw.Close()

	bitsPerPixel := 0

	switch cb {
	case cbP2:
		bitsPerPixel = 2
	case cbP1:
		bitsPerPixel = 1
	}

	// cr[*] and pr are the bytes for the current and previous row.
	// cr[0] is unfiltered (or equivalently, filtered with the ftNone filter).
	// cr[ft], for non-zero filter types ft, are buffers for transforming cr[0] under the
	// other PNG filter types. These buffers are allocated once and re-used for each row.
	// The +1 is for the per-row filter type, which is at cr[*][0].
	// This is useless
	b := m.Bounds()
	sz := 1 + (bitsPerPixel*b.Dx()+7)/8
	for i := range e.cr {
		if cap(e.cr[i]) < sz {
			e.cr[i] = make([]uint8, sz)
		} else {
			e.cr[i] = e.cr[i][:sz]
		}
		e.cr[i][0] = uint8(i)
	}
	cr := e.cr
	if cap(e.pr) < sz {
		e.pr = make([]uint8, sz)
	} else {
		e.pr = e.pr[:sz]
		zeroMemory(e.pr)
	}
	pr := e.pr


	for y := b.Min.Y; y < b.Max.Y; y++ {
		// Convert from colors to bytes.
		i := 1
		pi := m.(image.PalettedImage)

		var a uint8
		var c int
		pixelsPerByte := 8 / bitsPerPixel
		for x := b.Min.X; x < b.Max.X; x++ {
			a = a<<uint(bitsPerPixel) | pi.ColorIndexAt(x, y)
			c++
			if c == pixelsPerByte {
				cr[0][i] = a
				i += 1
				a = 0
				c = 0
			}
		}
		if c != 0 {
			for c != pixelsPerByte {
				a = a << uint(bitsPerPixel)
				c++
			}
			cr[0][i] = a
		}
		// It appears that paletted images should not be compressed,
		// but I'm too lazy to remove compression preparing code
		f := ftNone

		if _, err := e.zw.Write(cr[f]); err != nil {
			return err
		}
		// The current row for y is the previous row for y+1.
		pr, cr[0] = cr[0], pr
	}
	return nil
}

// Write the actual image data to one or more IDAT chunks.
func (e *encoder) writeIDATs() {
	e.writeType = 0
	if e.err != nil {
		return
	}
	if e.bw == nil {
		e.bw = bufio.NewWriterSize(e, 1<<15)
	} else {
		e.bw.Reset(e)
	}
	e.err = e.writeImage(e.bw, e.a.Frames[0].Image, e.cb, e.enc.CompressionLevel)
	if e.err != nil {
		return
	}
	e.err = e.bw.Flush()
}

// This function is required because we want the zero value of
// Encoder.CompressionLevel to map to zlib.DefaultCompression.
func levelToZlib(l CompressionLevel) int {
	switch l {
	case DefaultCompression:
		return zlib.DefaultCompression
	case NoCompression:
		return zlib.NoCompression
	case BestSpeed:
		return zlib.BestSpeed
	case BestCompression:
		return zlib.BestCompression
	default:
		return zlib.DefaultCompression
	}
}
