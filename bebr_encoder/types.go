package main

import (
	"bufio"
	"io"
)

// Encoder configures encoding PNG images.
type Encoder struct {
	CompressionLevel CompressionLevel

	// BufferPool optionally specifies a buffer pool to get temporary
	// EncoderBuffers when encoding an image.
	BufferPool EncoderBufferPool

	// CompressionWriter optionally provides a external zlib compression
	// writer for writing PNG image data.
	CompressionWriter func(w io.Writer) (CompressionWriter, error)
}

// CompressionWriter zlib compression writer interface.
type CompressionWriter interface {
	Write(p []byte) (n int, err error)
	Reset(w io.Writer)
	Close() error
}

// EncoderBufferPool is an interface for getting and returning temporary
// instances of the EncoderBuffer struct. This can be used to reuse buffers
// when encoding multiple images.
type EncoderBufferPool interface {
	Get() *EncoderBuffer
	Put(*EncoderBuffer)
}

// EncoderBuffer holds the buffers used for encoding PNG images.
type EncoderBuffer encoder

type encoder struct {
	enc       *Encoder
	writer    io.Writer
	a         BEBR
	writeType int // 0 = IDAT, 1 = fdAT
	seq       int // frame number
	cb        int // color mode
	err       error
	header    [8]byte
	footer    [4]byte
	tmp       [4 * 256]byte
	cr        [nFilter][]uint8
	pr        []uint8
	zw        CompressionWriter
	zwLevel   CompressionLevel
	bw        *bufio.Writer
}

// CompressionLevel indicates the compression level.
type CompressionLevel int
