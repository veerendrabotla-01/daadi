import wave
import struct
import math

def generate_wav(filename, duration_ms, frequency, volume=0.5, sample_rate=44100):
    num_samples = int(sample_rate * (duration_ms / 1000.0))
    with wave.open(filename, 'w') as wav_file:
        # Mono, 2 bytes per sample (16-bit), sample_rate
        wav_file.setparams((1, 2, sample_rate, num_samples, 'NONE', 'not compressed'))
        for i in range(num_samples):
            # Sine wave
            value = int(volume * 32767.0 * math.sin(2.0 * math.pi * frequency * i / sample_rate))
            # Exponential decay
            decay = 1.0 - (i / num_samples)
            value = int(value * decay)
            wav_file.writeframes(struct.pack('<h', value))

# Short click for place_piece
generate_wav('place_piece.wav', duration_ms=50, frequency=1000)
# Success ding for mill_formed
generate_wav('mill_formed.wav', duration_ms=300, frequency=800)
# Game over sound
generate_wav('game_over.wav', duration_ms=1000, frequency=200)
