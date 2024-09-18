/*
	BASSMIDI 2.4 Java class
	Copyright (c) 2006-2023 Un4seen Developments Ltd.

	See the BASSMIDI.CHM file for more detailed documentation
*/

package com.un4seen.bass;

import java.nio.ByteBuffer;
import android.os.ParcelFileDescriptor;

@SuppressWarnings({"all"})
public class BASSMIDI
{
	// Additional error codes returned by BASS_ErrorGetCode
	public static final int BASS_ERROR_MIDI_INCLUDE = 7000;

	// Additional BASS_SetConfig options
	public static final int BASS_CONFIG_MIDI_COMPACT = 0x10400;
	public static final int BASS_CONFIG_MIDI_VOICES = 0x10401;
	public static final int BASS_CONFIG_MIDI_AUTOFONT = 0x10402;
	public static final int BASS_CONFIG_MIDI_SAMPLETHREADS = 0x10406;
	public static final int BASS_CONFIG_MIDI_SAMPLEMEM = 0x10407;
	public static final int BASS_CONFIG_MIDI_SAMPLEREAD = 0x10408;
	public static final int BASS_CONFIG_MIDI_SAMPLELOADING = 0x1040a;

	// Additional BASS_SetConfigPtr options
	public static final int BASS_CONFIG_MIDI_DEFFONT = 0x10403;
	public static final int BASS_CONFIG_MIDI_SFZHEAD = 0x10409;

	// Additional sync types
	public static final int BASS_SYNC_MIDI_MARK = 0x10000;
	public static final int BASS_SYNC_MIDI_EVENT = 0x10004;
	public static final int BASS_SYNC_MIDI_TICK = 0x10005;

	// Additional BASS_MIDI_StreamCreateFile/etc flags
	public static final int BASS_MIDI_NODRUMPARAM = 0x400;
	public static final int BASS_MIDI_NOSYSRESET = 0x800;
	public static final int BASS_MIDI_DECAYEND = 0x1000;
	public static final int BASS_MIDI_NOFX = 0x2000;
	public static final int BASS_MIDI_DECAYSEEK = 0x4000;
	public static final int BASS_MIDI_NOCROP = 0x8000;
	public static final int BASS_MIDI_NOTEOFF1 = 0x10000;
	public static final int BASS_MIDI_ASYNC = 0x400000;
	public static final int BASS_MIDI_SINCINTER = 0x800000;

	// BASS_MIDI_FontInit flags
	public static final int BASS_MIDI_FONT_MMAP = 0x20000;
	public static final int BASS_MIDI_FONT_XGDRUMS = 0x40000;
	public static final int BASS_MIDI_FONT_NOFX = 0x80000;
	public static final int BASS_MIDI_FONT_LINATTMOD = 0x100000;
	public static final int BASS_MIDI_FONT_LINDECVOL = 0x200000;
	public static final int BASS_MIDI_FONT_NORAMPIN = 0x400000;
	public static final int BASS_MIDI_FONT_NOLIMITS = 0x800000;
	public static final int BASS_MIDI_FONT_MINFX = 0x1000000;

	public static class BASS_MIDI_FONT {
		public int font;        // soundfont
		public int preset;        // preset number (-1=all)
		public int bank;
	}

	public static class BASS_MIDI_FONTEX {
		public int font;        // soundfont
		public int spreset;        // source preset number
		public int sbank;        // source bank number
		public int dpreset;        // destination preset/program number
		public int dbank;        // destination bank number
		public int dbanklsb;    // destination bank number LSB
	}

	public static class BASS_MIDI_FONTEX2 {
		public int font;        // soundfont
		public int spreset;        // source preset number
		public int sbank;        // source bank number
		public int dpreset;        // destination preset/program number
		public int dbank;        // destination bank number
		public int dbanklsb;    // destination bank number LSB
		public int minchan;		// minimum channel number
		public int numchan;		// number of channels from minchan
	}

	// BASS_MIDI_StreamSet/GetFonts flag
	public static final int BASS_MIDI_FONT_EX = 0x1000000;	// BASS_MIDI_FONTEX (auto-detected)
	public static final int BASS_MIDI_FONT_EX2 = 0x2000000;	// BASS_MIDI_FONTEX2 (auto-detected)

	public static class BASS_MIDI_FONTINFO {
		public String name;
		public String copyright;
		public String comment;
		public int presets;        // number of presets/instruments
		public int samsize;        // total size (in bytes) of the sample data
		public int samload;        // amount of sample data currently loaded
		public int samtype;        // sample format (CTYPE) if packed
	}

	public static class BASS_MIDI_MARK {
		public int track;        // track containing marker
		public int pos;            // marker position
		public String text;        // marker text
	}

	public static class BASS_MIDI_MARKB {
		public int track;        // track containing marker
		public int pos;            // marker position
		public byte[] text;        // marker text
	}

	// Marker types
	public static final int BASS_MIDI_MARK_MARKER = 0;    // marker
	public static final int BASS_MIDI_MARK_CUE = 1;        // cue point
	public static final int BASS_MIDI_MARK_LYRIC = 2;        // lyric
	public static final int BASS_MIDI_MARK_TEXT = 3;        // text
	public static final int BASS_MIDI_MARK_TIMESIG = 4;    // time signature
	public static final int BASS_MIDI_MARK_KEYSIG = 5;    // key signature
	public static final int BASS_MIDI_MARK_COPY = 6;        // copyright notice
	public static final int BASS_MIDI_MARK_TRACK = 7;        // track name
	public static final int BASS_MIDI_MARK_INST = 8;        // instrument name
	public static final int BASS_MIDI_MARK_TRACKSTART = 9; // track start (SMF2)
	public static final int BASS_MIDI_MARK_TICK = 0x10000; // flag: get position in ticks (otherwise bytes)

	// MIDI events
	public static final int MIDI_EVENT_NOTE = 1;
	public static final int MIDI_EVENT_PROGRAM = 2;
	public static final int MIDI_EVENT_CHANPRES = 3;
	public static final int MIDI_EVENT_PITCH = 4;
	public static final int MIDI_EVENT_PITCHRANGE = 5;
	public static final int MIDI_EVENT_DRUMS = 6;
	public static final int MIDI_EVENT_FINETUNE = 7;
	public static final int MIDI_EVENT_COARSETUNE = 8;
	public static final int MIDI_EVENT_MASTERVOL = 9;
	public static final int MIDI_EVENT_BANK = 10;
	public static final int MIDI_EVENT_MODULATION = 11;
	public static final int MIDI_EVENT_VOLUME = 12;
	public static final int MIDI_EVENT_PAN = 13;
	public static final int MIDI_EVENT_EXPRESSION = 14;
	public static final int MIDI_EVENT_SUSTAIN = 15;
	public static final int MIDI_EVENT_SOUNDOFF = 16;
	public static final int MIDI_EVENT_RESET = 17;
	public static final int MIDI_EVENT_NOTESOFF = 18;
	public static final int MIDI_EVENT_PORTAMENTO = 19;
	public static final int MIDI_EVENT_PORTATIME = 20;
	public static final int MIDI_EVENT_PORTANOTE = 21;
	public static final int MIDI_EVENT_MODE = 22;
	public static final int MIDI_EVENT_REVERB = 23;
	public static final int MIDI_EVENT_CHORUS = 24;
	public static final int MIDI_EVENT_CUTOFF = 25;
	public static final int MIDI_EVENT_RESONANCE = 26;
	public static final int MIDI_EVENT_RELEASE = 27;
	public static final int MIDI_EVENT_ATTACK = 28;
	public static final int MIDI_EVENT_DECAY = 29;
	public static final int MIDI_EVENT_REVERB_MACRO = 30;
	public static final int MIDI_EVENT_CHORUS_MACRO = 31;
	public static final int MIDI_EVENT_REVERB_TIME = 32;
	public static final int MIDI_EVENT_REVERB_DELAY = 33;
	public static final int MIDI_EVENT_REVERB_LOCUTOFF = 34;
	public static final int MIDI_EVENT_REVERB_HICUTOFF = 35;
	public static final int MIDI_EVENT_REVERB_LEVEL = 36;
	public static final int MIDI_EVENT_CHORUS_DELAY = 37;
	public static final int MIDI_EVENT_CHORUS_DEPTH = 38;
	public static final int MIDI_EVENT_CHORUS_RATE = 39;
	public static final int MIDI_EVENT_CHORUS_FEEDBACK = 40;
	public static final int MIDI_EVENT_CHORUS_LEVEL = 41;
	public static final int MIDI_EVENT_CHORUS_REVERB = 42;
	public static final int MIDI_EVENT_USERFX = 43;
	public static final int MIDI_EVENT_USERFX_LEVEL = 44;
	public static final int MIDI_EVENT_USERFX_REVERB = 45;
	public static final int MIDI_EVENT_USERFX_CHORUS = 46;
	public static final int MIDI_EVENT_DRUM_FINETUNE = 50;
	public static final int MIDI_EVENT_DRUM_COARSETUNE = 51;
	public static final int MIDI_EVENT_DRUM_PAN = 52;
	public static final int MIDI_EVENT_DRUM_REVERB = 53;
	public static final int MIDI_EVENT_DRUM_CHORUS = 54;
	public static final int MIDI_EVENT_DRUM_CUTOFF = 55;
	public static final int MIDI_EVENT_DRUM_RESONANCE = 56;
	public static final int MIDI_EVENT_DRUM_LEVEL = 57;
	public static final int MIDI_EVENT_DRUM_USERFX = 58;
	public static final int MIDI_EVENT_SOFT = 60;
	public static final int MIDI_EVENT_SYSTEM = 61;
	public static final int MIDI_EVENT_TEMPO = 62;
	public static final int MIDI_EVENT_SCALETUNING = 63;
	public static final int MIDI_EVENT_CONTROL = 64;
	public static final int MIDI_EVENT_CHANPRES_VIBRATO = 65;
	public static final int MIDI_EVENT_CHANPRES_PITCH = 66;
	public static final int MIDI_EVENT_CHANPRES_FILTER = 67;
	public static final int MIDI_EVENT_CHANPRES_VOLUME = 68;
	public static final int MIDI_EVENT_MOD_VIBRATO = 69;
	public static final int MIDI_EVENT_MODRANGE = 69;
	public static final int MIDI_EVENT_BANK_LSB = 70;
	public static final int MIDI_EVENT_KEYPRES = 71;
	public static final int MIDI_EVENT_KEYPRES_VIBRATO = 72;
	public static final int MIDI_EVENT_KEYPRES_PITCH = 73;
	public static final int MIDI_EVENT_KEYPRES_FILTER = 74;
	public static final int MIDI_EVENT_KEYPRES_VOLUME = 75;
	public static final int MIDI_EVENT_SOSTENUTO = 76;
	public static final int MIDI_EVENT_MOD_PITCH = 77;
	public static final int MIDI_EVENT_MOD_FILTER = 78;
	public static final int MIDI_EVENT_MOD_VOLUME = 79;
	public static final int MIDI_EVENT_VIBRATO_RATE = 80;
	public static final int MIDI_EVENT_VIBRATO_DEPTH = 81;
	public static final int MIDI_EVENT_VIBRATO_DELAY = 82;
	public static final int MIDI_EVENT_MASTER_FINETUNE = 83;
	public static final int MIDI_EVENT_MASTER_COARSETUNE = 84;
	public static final int MIDI_EVENT_MIXLEVEL = 0x10000;
	public static final int MIDI_EVENT_TRANSPOSE = 0x10001;
	public static final int MIDI_EVENT_SYSTEMEX = 0x10002;
	public static final int MIDI_EVENT_SPEED = 0x10004;
	public static final int MIDI_EVENT_DEFDRUMS = 0x10006;

	public static final int MIDI_EVENT_END = 0;
	public static final int MIDI_EVENT_END_TRACK = 0x10003;

	public static final int MIDI_EVENT_NOTES = 0x20000;
	public static final int MIDI_EVENT_VOICES = 0x20001;

	public static final int MIDI_SYSTEM_DEFAULT = 0;
	public static final int MIDI_SYSTEM_GM1 = 1;
	public static final int MIDI_SYSTEM_GM2 = 2;
	public static final int MIDI_SYSTEM_XG = 3;
	public static final int MIDI_SYSTEM_GS = 4;

	public static class BASS_MIDI_EVENT {
		public int event;        // MIDI_EVENT_xxx
		public int param;
		public int chan;
		public int tick;        // event position (ticks)
		public int pos;            // event position (bytes)
	}

	// BASS_MIDI_StreamEvents modes
	public static final int BASS_MIDI_EVENTS_STRUCT = 0; // BASS_MIDI_EVENT structures
	public static final int BASS_MIDI_EVENTS_RAW = 0x10000; // raw MIDI event data
	public static final int BASS_MIDI_EVENTS_SYNC = 0x1000000; // flag: trigger event syncs
	public static final int BASS_MIDI_EVENTS_NORSTATUS = 0x2000000; // flag: no running status
	public static final int BASS_MIDI_EVENTS_CANCEL = 0x4000000; // flag: cancel pending events
	public static final int BASS_MIDI_EVENTS_TIME = 0x8000000; // flag: delta-time info is present
	public static final int BASS_MIDI_EVENTS_ABSTIME = 0x10000000; // flag: absolute time info is present
	public static final int BASS_MIDI_EVENTS_ASYNC = 0x20000000; // flag: process asynchronously
	public static final int BASS_MIDI_EVENTS_FILTER = 0x40000000; // flag: apply filtering
	public static final int BASS_MIDI_EVENTS_FLUSH = 0x80000000; // flag: flush async events

	// BASS_MIDI_StreamGetChannel special channels
	public static final int BASS_MIDI_CHAN_CHORUS = -1;
	public static final int BASS_MIDI_CHAN_REVERB = -2;
	public static final int BASS_MIDI_CHAN_USERFX = -3;

	// BASS_CHANNELINFO type
	public static final int BASS_CTYPE_STREAM_MIDI = 0x10d00;

	// Additional attributes
	public static final int BASS_ATTRIB_MIDI_PPQN = 0x12000;
	public static final int BASS_ATTRIB_MIDI_CPU = 0x12001;
	public static final int BASS_ATTRIB_MIDI_CHANS = 0x12002;
	public static final int BASS_ATTRIB_MIDI_VOICES = 0x12003;
	public static final int BASS_ATTRIB_MIDI_VOICES_ACTIVE = 0x12004;
	public static final int BASS_ATTRIB_MIDI_STATE = 0x12005;
	public static final int BASS_ATTRIB_MIDI_SRC = 0x12006;
	public static final int BASS_ATTRIB_MIDI_KILL = 0x12007;
	public static final int BASS_ATTRIB_MIDI_SPEED = 0x12008;
	public static final int BASS_ATTRIB_MIDI_REVERB = 0x12009;
	public static final int BASS_ATTRIB_MIDI_VOL = 0x1200a;
	public static final int BASS_ATTRIB_MIDI_TRACK_VOL = 0x12100; // + track #

	// Additional tag type
	public static final int BASS_TAG_MIDI_TRACK = 0x11000;    // + track #, track text : array of null-terminated ANSI strings

	// BASS_ChannelGetLength/GetPosition/SetPosition mode
	public static final int BASS_POS_MIDI_TICK = 2;        // tick position

	public interface MIDIFILTERPROC {
		boolean MIDIFILTERPROC(int handle, int track, BASS_MIDI_EVENT event, boolean seeking, Object user);
		/* Event filtering callback function.
		handle : MIDI stream handle
		track  : Track containing the event
		event  : The event
		seeking: true = the event is being processed while seeking, false = it is being played
		user   : The 'user' parameter value given when calling BASS_MIDI_StreamSetFilter
		RETURN : true = process the event, false = drop the event */
	}

	// BASS_MIDI_FontLoadEx flags
	public static final int BASS_MIDI_FONTLOAD_NOWAIT = 1; // don't want for the samples to load
	public static final int BASS_MIDI_FONTLOAD_COMPACT = 2; // compact samples
	public static final int BASS_MIDI_FONTLOAD_NOLOAD = 4; // don't load (only compact)
	public static final int BASS_MIDI_FONTLOAD_TIME = 8; // length is in milliseconds
	public static final int BASS_MIDI_FONTLOAD_KEEPDEC = 16; // keep decoders

	public static class BASS_MIDI_DEVICEINFO {
		public String name;    // description
		public int id;
		public int flags;
	}

	public static native int BASS_MIDI_GetVersion();

	public static native int BASS_MIDI_StreamCreate(int channels, int flags, int freq);
	public static native int BASS_MIDI_StreamCreateFile(String file, long offset, long length, int flags, int freq);
	public static native int BASS_MIDI_StreamCreateFile(ByteBuffer file, long offset, long length, int flags, int freq);
	public static native int BASS_MIDI_StreamCreateFile(BASS.Asset file, long offset, long length, int flags, int freq);
	public static native int BASS_MIDI_StreamCreateFile(ParcelFileDescriptor file, long offset, long length, int flags, int freq);
	public static native int BASS_MIDI_StreamCreateURL(String url, int offset, int flags, BASS.DOWNLOADPROC proc, Object user, int freq);
	public static native int BASS_MIDI_StreamCreateFileUser(int system, int flags, BASS.BASS_FILEPROCS procs, Object user, int freq);
	public static native int BASS_MIDI_StreamCreateEvents(BASS_MIDI_EVENT[] events, int ppqn, int flags, int freq);
	public static native boolean BASS_MIDI_StreamGetMark(int handle, int type, int index, BASS_MIDI_MARK mark);
	public static native boolean BASS_MIDI_StreamGetMark(int handle, int type, int index, BASS_MIDI_MARKB mark);
	public static native int BASS_MIDI_StreamGetMarks(int handle, int track, int type, BASS_MIDI_MARK[] marks);
	public static native int BASS_MIDI_StreamGetMarks(int handle, int track, int type, BASS_MIDI_MARKB[] marks);
	public static native boolean BASS_MIDI_StreamSetFonts(int handle, BASS_MIDI_FONT[] fonts, int count);
	public static native boolean BASS_MIDI_StreamSetFonts(int handle, BASS_MIDI_FONTEX[] fonts, int count);
	public static native boolean BASS_MIDI_StreamSetFonts(int handle, BASS_MIDI_FONTEX2[] fonts, int count);
	public static native int BASS_MIDI_StreamGetFonts(int handle, BASS_MIDI_FONT[] fonts, int count);
	public static native int BASS_MIDI_StreamGetFonts(int handle, BASS_MIDI_FONTEX[] fonts, int count);
	public static native int BASS_MIDI_StreamGetFonts(int handle, BASS_MIDI_FONTEX2[] fonts, int count);
	public static native boolean BASS_MIDI_StreamLoadSamples(int handle);
	public static native boolean BASS_MIDI_StreamEvent(int handle, int chan, int event, int param);
	public static native int BASS_MIDI_StreamEvents(int handle, int mode, BASS_MIDI_EVENT[] events, int length);
	public static native int BASS_MIDI_StreamEvents(int handle, int mode, ByteBuffer events, int length);
	public static native int BASS_MIDI_StreamGetEvent(int handle, int chan, int event);
	public static native int BASS_MIDI_StreamGetEvents(int handle, int track, int filter, BASS_MIDI_EVENT[] events);
	public static native int BASS_MIDI_StreamGetEventsEx(int handle, int track, int filter, BASS_MIDI_EVENT[] events, int start, int count);
	public static native boolean BASS_MIDI_StreamGetPreset(int handle, int chan, BASS_MIDI_FONT font);
	public static native int BASS_MIDI_StreamGetChannel(int handle, int chan);
	public static native boolean BASS_MIDI_StreamSetFilter(int handle, boolean seeking, MIDIFILTERPROC proc, Object user);

	public static native int BASS_MIDI_FontInit(String file, int flags);
	public static native int BASS_MIDI_FontInit(ByteBuffer file, int flags);
	public static native int BASS_MIDI_FontInit(BASS.Asset file, int flags);
	public static native int BASS_MIDI_FontInit(ParcelFileDescriptor file, int flags);
	public static native int BASS_MIDI_FontInitUser(BASS.BASS_FILEPROCS procs, Object user, int flags);
	public static native boolean BASS_MIDI_FontFree(int handle);
	public static native boolean BASS_MIDI_FontGetInfo(int handle, BASS_MIDI_FONTINFO info);
	public static native boolean BASS_MIDI_FontGetPresets(int handle, int[] presets);
	public static native String BASS_MIDI_FontGetPreset(int handle, int preset, int bank);
	public static native boolean BASS_MIDI_FontLoad(int handle, int preset, int bank);
	public static native boolean BASS_MIDI_FontLoadEx(int handle, int preset, int bank, int length, int flags);
	public static native boolean BASS_MIDI_FontUnload(int handle, int preset, int bank);
	public static native boolean BASS_MIDI_FontCompact(int handle);
	public static native boolean BASS_MIDI_FontUnpack(int handle, String outfile, int flags);
	public static native int BASS_MIDI_FontFlags(int handle, int flags, int mask);
	public static native boolean BASS_MIDI_FontSetVolume(int handle, float volume);
	public static native float BASS_MIDI_FontGetVolume(int handle);

	public static native int BASS_MIDI_ConvertEvents(ByteBuffer data, int length, BASS_MIDI_EVENT[] events, int count, int flags);

	static {
		System.loadLibrary("bassmidi");
	}
}
