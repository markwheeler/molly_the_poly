// CroneEngine_MollyThePoly
// Classic polysynth with a Juno-6 voice structure, the extra modulation of a Jupiter-8, and CS-80 inspired ring modulation.
// v1.2.0 Mark Eats

Engine_MollyThePoly : CroneEngine {

	classvar maxNumVoices = 10;
	classvar maxAmp = 11;
	var voiceGroup;
	var voiceList;
	var lfo;
	var mixer;

	var lfoBus;
	var ringModBus;
	var mixerBus;

	var pitchBendRatio = 1;
	var pressure = 0;
	var timbre = 0;

	var oscWaveShape = 0;
	var pulseWidth = 0;
	var pulseWidthModEnv = 0;
	var pulseWidthModLfo = 0;
	var pulseWidthModPressure = 0;
	var pulseWidthModTimbre = 0;
	var freqModLfo = 0;
	var freqModEnv = 0;
	var lastFreq = 0;
	var glide = 0;
	var mainOscLevel = 1;
	var mainOscLevelModEnv = 0;
	var mainOscLevelModLfo = 0;
	var mainOscLevelModPressure = 0;
	var mainOscLevelModTimbre = 0;
	var subOscLevel = 0;
	var subOscLevelModEnv = 0;
	var subOscLevelModLfo = 0;
	var subOscLevelModPressure = 0;
	var subOscLevelModTimbre = 0;
	var subOscDetune = 0;
	var noiseLevel = 0;
	var noiseLevelModEnv = 0;
	var noiseLevelModLfo = 0;
	var noiseLevelModPressure = 0;
	var noiseLevelModTimbre = 0;
	var hpFilterCutoff = 10;
	var lpFilterType = 0;
	var lpFilterCutoff = 440;
	var lpFilterResonance = 0.2;
	var lpFilterCutoffEnvSelect = 0;
	var lpFilterCutoffModEnv = 0;
	var lpFilterCutoffModLfo = 0;
	var lpFilterCutoffModPressure = 0;
	var lpFilterCutoffModTimbre = 0;
	var lpFilterTracking = 1;
	var lfoFade = 0;
	var env1Attack = 0.01;
	var env1Decay = 0.3;
	var env1Sustain = 0.5;
	var env1Release = 0.5;
	var env2Attack = 0.01;
	var env2Decay = 0.3;
	var env2Sustain = 0.5;
	var env2Release = 0.5;
	var amp = 0.5;
	var ampModLfo = 0;
	var ampModPressure = 0;
	var ampModTimbre = 0;
	var ringModFade = 0;
	var ringModMix = 0;
	var ringModMixModEnv = 0;
	var ringModMixModLfo = 0;
	var ringModMixModPressure = 0;
	var ringModMixModTimbre = 0;

	*new { arg context, doneCallback;
		^super.new(context, doneCallback);
	}

	alloc {

		voiceGroup = Group.new(context.xg);
		voiceList = List.new();

		lfoBus = Bus.control(context.server, 1);
		ringModBus = Bus.audio(context.server, 1);
		mixerBus = Bus.audio(context.server, 1);


		// Synth voice
		SynthDef(\voice, {
			arg out, lfoIn, ringModIn, freq = 440, pitchBendRatio = 1, gate = 0, killGate = 1, vel = 1, pressure, timbre,
			oscWaveShape, pulseWidth, pulseWidthModEnv, pulseWidthModLfo, pulseWidthModPressure, pulseWidthModTimbre,
			freqModLfo, freqModEnv, lastFreq, glide,
			mainOscLevel, mainOscLevelModEnv, mainOscLevelModLfo, mainOscLevelModPressure, mainOscLevelModTimbre,
			subOscLevel, subOscLevelModEnv, subOscLevelModLfo, subOscLevelModPressure, subOscLevelModTimbre, subOscDetune,
			noiseLevel, noiseLevelModEnv, noiseLevelModLfo, noiseLevelModPressure, noiseLevelModTimbre,
			hpFilterCutoff, lpFilterCutoff, lpFilterResonance, lpFilterType,
			lpFilterCutoffEnvSelect, lpFilterCutoffModEnv, lpFilterCutoffModLfo, lpFilterCutoffModPressure, lpFilterCutoffModTimbre, lpFilterTracking,
			lfoFade, env1Attack, env1Decay, env1Sustain, env1Release, env2Attack, env2Decay, env2Sustain, env2Release,
			amp = 0.5, ampModLfo, ampModPressure, ampModTimbre,
			ringModFade, ringModMix, ringModMixModEnv, ringModMixModLfo, ringModMixModPressure, ringModMixModTimbre;
			var i_nyquist = SampleRate.ir * 0.5, i_cFreq = 48.midicps, signal, killEnvelope, controlLag = 0.005,
			lfo, ringMod, oscArray, freqModRatio, mainOscDriftLfo, subOscDriftLfo, filterCutoffRatio, filterCutoffModRatio,
			envelope1, envelope2;

			// LFO in
			lfo = Line.kr(start: (lfoFade < 0), end: (lfoFade >= 0), dur: lfoFade.abs, mul: In.kr(lfoIn, 1));
			ringMod = Line.kr(start: (ringModFade < 0), end: (ringModFade >= 0), dur: ringModFade.abs, mul: In.ar(ringModIn, 1));

			// Lag and map inputs

			freq = XLine.kr(start: lastFreq, end: freq, dur: glide + 0.001);
			freq = Lag.kr(freq * pitchBendRatio, 0.005);
			pressure = Lag.kr(pressure, controlLag);
			timbre = Lag.kr(timbre, controlLag);

			pulseWidth = Lag.kr(pulseWidth, controlLag);
			mainOscLevel = Lag.kr(mainOscLevel, controlLag);
			subOscLevel = Lag.kr(subOscLevel, controlLag);
			subOscDetune = Lag.kr(subOscDetune, controlLag);
			noiseLevel = Lag.kr(noiseLevel, controlLag);

			hpFilterCutoff = Lag.kr(hpFilterCutoff, controlLag);
			lpFilterCutoff = Lag.kr(lpFilterCutoff, controlLag);
			lpFilterResonance = Lag.kr(lpFilterResonance, controlLag);
			lpFilterType = Lag.kr(lpFilterType, 0.01);

			amp = Lag.kr(amp, controlLag);
			ringModMix = Lag.kr(ringModMix, controlLag);

			// Envelopes
			killGate = killGate + Impulse.kr(0); // Make sure doneAction fires
			killEnvelope = EnvGen.kr(envelope: Env.asr( 0, 1, 0.01), gate: killGate, doneAction: Done.freeSelf);

			envelope1 = EnvGen.ar(envelope: Env.adsr( env1Attack, env1Decay, env1Sustain, env1Release), gate: gate);
			envelope2 = EnvGen.ar(envelope: Env.adsr( env2Attack, env2Decay, env2Sustain, env2Release), gate: gate, doneAction: Done.freeSelf);

			// Main osc

			// Note: Would be ideal to do this exponentially but its a surprisingly big perf hit
			freqModRatio = ((lfo * freqModLfo) + (envelope1 * freqModEnv));
			freqModRatio = Select.ar(freqModRatio >= 0, [
				freqModRatio.linlin(-2, 0, 0.25, 1),
				freqModRatio.linlin(0, 2, 1, 4)
			]);
			freq = (freq * freqModRatio).clip(20, i_nyquist);

			mainOscDriftLfo = LFNoise2.kr(freq: 0.1, mul: 0.001, add: 1);

			pulseWidth = (pulseWidth + (envelope1 * pulseWidthModEnv) + (lfo * pulseWidthModLfo) + (pressure * pulseWidthModPressure) + (timbre * pulseWidthModTimbre)).clip;

			oscArray = [
				VarSaw.ar(freq * mainOscDriftLfo),
				Saw.ar(freq * mainOscDriftLfo),
				Pulse.ar(freq * mainOscDriftLfo, width: 0.5 + (pulseWidth * 0.49)),
			];
			signal = Select.ar(oscWaveShape, oscArray) * (mainOscLevel + (envelope1 * mainOscLevelModEnv) + (lfo * mainOscLevelModLfo) + (pressure * mainOscLevelModPressure) + (timbre * mainOscLevelModTimbre)).clip;

			// Sub osc and noise
			subOscDriftLfo = LFNoise2.kr(freq: 0.1, mul: 0.0008, add: 1);
			signal = SelectX.ar((subOscLevel + (envelope1 * subOscLevelModEnv) + (lfo * subOscLevelModLfo) + (pressure * subOscLevelModPressure) + (timbre * subOscLevelModTimbre)).clip * 0.5, [signal, Pulse.ar(freq * 0.5 * subOscDetune.midiratio * subOscDriftLfo, width: 0.5)]);
			signal = SelectX.ar((noiseLevel + (envelope1 * noiseLevelModEnv) + (lfo * noiseLevelModLfo) + (pressure * noiseLevelModPressure) + (timbre * noiseLevelModTimbre)).clip * 0.5, [signal, WhiteNoise.ar()]);
			signal = signal + PinkNoise.ar(0.007);

			// HP Filter
			filterCutoffRatio = Select.kr((freq < i_cFreq), [
				i_cFreq + (freq - i_cFreq),
				i_cFreq - (i_cFreq - freq)
			]);
			filterCutoffRatio = filterCutoffRatio / i_cFreq;
			hpFilterCutoff = (hpFilterCutoff * filterCutoffRatio).clip(10, 20000);
			signal = HPF.ar(in: signal, freq: hpFilterCutoff);

			// LP Filter
			filterCutoffRatio = Select.kr((freq < i_cFreq), [
				i_cFreq + ((freq - i_cFreq) * lpFilterTracking),
				i_cFreq - ((i_cFreq - freq) * lpFilterTracking)
			]);
			filterCutoffRatio = filterCutoffRatio / i_cFreq;
			lpFilterCutoff = lpFilterCutoff * filterCutoffRatio;

			// Note: Again, would prefer this to be exponential
			filterCutoffModRatio = (((Select.ar(lpFilterCutoffEnvSelect, [envelope1, envelope2]) * lpFilterCutoffModEnv) + (pressure * lpFilterCutoffModPressure) + (timbre * lpFilterCutoffModTimbre)) * 2) + (lfo * lpFilterCutoffModLfo);
			filterCutoffModRatio = Select.ar(filterCutoffModRatio >= 0, [
				filterCutoffModRatio.linlin(-3, 0, 0.08333333333, 1),
				filterCutoffModRatio.linlin(0, 3, 1, 12)
			]);
			lpFilterCutoff = (lpFilterCutoff * filterCutoffModRatio).clip(20, 20000);

			signal = RLPF.ar(in: signal, freq: lpFilterCutoff, rq: lpFilterResonance.linexp(0, 1, 1, 0.05));
			signal = SelectX.ar(lpFilterType, [signal, RLPF.ar(in: signal, freq: lpFilterCutoff, rq: lpFilterResonance.linexp(0, 1, 1, 0.32))]);

			// Amp
			signal = signal * envelope2 * killEnvelope;
			signal = signal * (lfo * ampModLfo).range(1 - ampModLfo, 1);
			amp = (amp + (((pressure * ampModPressure) + (timbre * ampModTimbre)) * maxAmp)).clip(0, maxAmp);
			signal = signal * vel * amp;

			// Ring mod
			signal = SelectX.ar((ringModMix + (envelope1 * ringModMixModEnv) + (lfo * ringModMixModLfo) + (pressure * ringModMixModPressure) + (timbre * ringModMixModTimbre)).clip, [signal, signal * ringMod]);

			Out.ar(out, signal);
		}).add;


		// LFO
		lfo = SynthDef(\lfo, {
			arg lfoOut, ringModOut, lfoFreq = 5, lfoWaveShape = 0, ringModFreq = 50;
			var lfo, lfoOscArray, ringMod, controlLag = 0.005;

			// Lag inputs
			lfoFreq = Lag.kr(lfoFreq, controlLag);
			ringModFreq = Lag.kr(ringModFreq, controlLag);

			lfoOscArray = [
				SinOsc.kr(lfoFreq),
				LFTri.kr(lfoFreq),
				LFSaw.kr(lfoFreq),
				LFPulse.kr(lfoFreq, mul: 2, add: -1),
				LFNoise0.kr(lfoFreq)
			];

			lfo = Select.kr(lfoWaveShape, lfoOscArray);
			lfo = Lag.kr(lfo, 0.005);

			Out.kr(lfoOut, lfo);

			ringMod = SinOsc.ar(ringModFreq);
			Out.ar(ringModOut, ringMod);

		}).play(target:context.xg, args: [\lfoOut, lfoBus, \ringModOut, ringModBus], addAction: \addToHead);


		// Mixer and chorus
		mixer = SynthDef(\mixer, {
			arg in, out, chorusMix = 0;
			var signal, chorus, chorusPreProcess, chorusLfo, chorusPreDelay = 0.01, chorusDepth = 0.0053, chorusDelay, controlLag = 0.005;

			// Lag inputs
			chorusMix = Lag.kr(chorusMix, controlLag);

			signal = In.ar(in, 1) * 0.4;

			// Bass boost
			signal = BLowShelf.ar(signal, freq: 400, rs: 1, db: 2, mul: 1, add: 0);

			// Compression etc
			signal = LPF.ar(in: signal, freq: 14000);
			signal = CompanderD.ar(in: signal, thresh: 0.4, slopeBelow: 1, slopeAbove: 0.25, clampTime: 0.002, relaxTime: 0.01);
			signal = tanh(signal).softclip;

			// Chorus

			chorusPreProcess = signal + (signal * WhiteNoise.ar(0.004));

			chorusLfo = LFPar.kr(chorusMix.linlin(0.7, 1, 0.5, 0.75));
			chorusDelay = chorusPreDelay + chorusMix.linlin(0.5, 1, chorusDepth, chorusDepth * 0.75);

			chorus = Array.with(
				DelayC.ar(in: chorusPreProcess, maxdelaytime: chorusPreDelay + chorusDepth, delaytime: chorusLfo.range(chorusPreDelay, chorusDelay)),
				DelayC.ar(in: chorusPreProcess, maxdelaytime: chorusPreDelay + chorusDepth, delaytime: chorusLfo.range(chorusDelay, chorusPreDelay))
			);
			chorus = LPF.ar(chorus, 14000);

			Out.ar(bus: out, channelsArray: SelectX.ar(chorusMix * 0.5, [signal.dup, chorus]));

		}).play(target:context.xg, args: [\in, mixerBus, \out, context.out_b], addAction: \addToTail);

		this.addCommands;
	}

	// Commands

	setArgOnVoice {
		arg voiceId, name, value;
		var voice = voiceList.detect{arg v; v.id == voiceId};
		if(voice.notNil, {
			voice.theSynth.set(name, value);
		});
	}

	addCommands {
		// noteOn(id, freq, vel)
		this.addCommand(\noteOn, "iff", {
			arg msg;
			var id = msg[1], freq = msg[2], vel = msg[3] ?? 1;
			var voiceToRemove, newVoice;

			// Remove voice if ID matches or there are too many
			voiceToRemove = voiceList.detect{arg item; item.id == id};
			if(voiceToRemove.isNil && (voiceList.size >= maxNumVoices), {
				voiceToRemove = voiceList.detect{arg v; v.gate == 0};
				if(voiceToRemove.isNil, {
					voiceToRemove = voiceList.last;
				});
			});
			if(voiceToRemove.notNil, {
				voiceToRemove.theSynth.set(\gate, 0);
				voiceToRemove.theSynth.set(\killGate, 0);
				voiceList.remove(voiceToRemove);
			});

			if(lastFreq == 0, {
				lastFreq = freq;
			});

			// Add new voice
			context.server.makeBundle(nil, {
				newVoice = (id: id, theSynth: Synth.new(defName: \voice, args: [
					\out, mixerBus,
					\lfoIn, lfoBus,
					\ringModIn, ringModBus,
					\freq, freq,
					\pitchBendRatio, pitchBendRatio,
					\pressure, pressure,
					\timbre, timbre,
					\gate, 1,
					\vel, vel.linlin(0, 1, 0.3, 1),
					\oscWaveShape, oscWaveShape,
					\pulseWidth, pulseWidth,
					\pulseWidthModEnv, pulseWidthModEnv,
					\pulseWidthModLfo, pulseWidthModLfo,
					\pulseWidthModPressure, pulseWidthModPressure,
					\pulseWidthModTimbre, pulseWidthModTimbre,
					\freqModLfo, freqModLfo,
					\freqModEnv, freqModEnv,
					\lastFreq, lastFreq,
					\glide, glide,
					\mainOscLevel, mainOscLevel,
					\mainOscLevelModEnv, mainOscLevelModEnv,
					\mainOscLevelModLfo, mainOscLevelModLfo,
					\mainOscLevelModPressure, mainOscLevelModPressure,
					\mainOscLevelModTimbre, mainOscLevelModTimbre,
					\subOscLevel, subOscLevel,
					\subOscLevelModEnv, subOscLevelModEnv,
					\subOscLevelModLfo, subOscLevelModLfo,
					\subOscLevelModPressure, subOscLevelModPressure,
					\subOscLevelModTimbre, subOscLevelModTimbre,
					\subOscDetune, subOscDetune,
					\noiseLevel, noiseLevel,
					\noiseLevelModEnv, noiseLevelModEnv,
					\noiseLevelModLfo, noiseLevelModLfo,
					\noiseLevelModPressure, noiseLevelModPressure,
					\noiseLevelModTimbre, noiseLevelModTimbre,
					\hpFilterCutoff, hpFilterCutoff,
					\lpFilterType, lpFilterType,
					\lpFilterCutoff, lpFilterCutoff,
					\lpFilterResonance, lpFilterResonance,
					\lpFilterCutoffEnvSelect, lpFilterCutoffEnvSelect,
					\lpFilterCutoffModEnv, lpFilterCutoffModEnv,
					\lpFilterCutoffModLfo, lpFilterCutoffModLfo,
					\lpFilterCutoffModPressure, lpFilterCutoffModPressure,
					\lpFilterCutoffModTimbre, lpFilterCutoffModTimbre,
					\lpFilterTracking, lpFilterTracking,
					\lfoFade, lfoFade,
					\env1Attack, env1Attack,
					\env1Decay, env1Decay,
					\env1Sustain, env1Sustain,
					\env1Release, env1Release,
					\env2Attack, env2Attack,
					\env2Decay, env2Decay,
					\env2Sustain, env2Sustain,
					\env2Release, env2Release,
					\amp, amp,
					\ampModLfo, ampModLfo,
					\ampModPressure, ampModPressure,
					\ampModTimbre, ampModTimbre,
					\ringModFade, ringModFade,
					\ringModMix, ringModMix,
					\ringModMixModEnv, ringModMixModEnv,
					\ringModMixModLfo, ringModMixModLfo,
					\ringModMixModPressure, ringModMixModPressure,
					\ringModMixModTimbre, ringModMixModTimbre,
				], target: voiceGroup).onFree({ voiceList.remove(newVoice); }), gate: 1);

				voiceList.addFirst(newVoice);
				lastFreq = freq;
			});
		});

		// noteOff(id)
		this.addCommand(\noteOff, "i", {
			arg msg;
			var voice = voiceList.detect{arg v; v.id == msg[1]};
			if(voice.notNil, {
				voice.theSynth.set(\gate, 0);
				voice.gate = 0;
			});
		});

		// noteOffAll()
		this.addCommand(\noteOffAll, "", {
			arg msg;
			voiceGroup.set(\gate, 0);
			voiceList.do({ arg v; v.gate = 0; });
		});

		// noteKill(id)
		this.addCommand(\noteKill, "i", {
			arg msg;
			var voice = voiceList.detect{arg v; v.id == msg[1]};
			if(voice.notNil, {
				voice.theSynth.set(\gate, 0);
				voice.theSynth.set(\killGate, 0);
				voiceList.remove(voice);
			});
		});

		// noteKillAll()
		this.addCommand(\noteKillAll, "", {
			arg msg;
			voiceGroup.set(\gate, 0);
			voiceGroup.set(\killGate, 0);
			voiceList.clear;
		});

		// pitchBend(id, ratio)
		this.addCommand(\pitchBend, "if", {
			arg msg;
			this.setArgOnVoice(msg[1], \pitchBendRatio, msg[2]);
		});

		// pitchBendAll(ratio)
		this.addCommand(\pitchBendAll, "f", {
			arg msg;
			pitchBendRatio = msg[1];
			voiceGroup.set(\pitchBendRatio, pitchBendRatio);
		});

		// pressure(id, pressure)
		this.addCommand(\pressure, "if", {
			arg msg;
			this.setArgOnVoice(msg[1], \pressure, msg[2]);
		});

		// pressureAll(pressure)
		this.addCommand(\pressureAll, "f", {
			arg msg;
			pressure = msg[1];
			voiceGroup.set(\pressure, pressure);
		});

		// timbre(id, timbre)
		this.addCommand(\timbre, "if", {
			arg msg;
			this.setArgOnVoice(msg[1], \timbre, msg[2]);
		});

		// timbreAll(timbre)
		this.addCommand(\timbreAll, "f", {
			arg msg;
			timbre = msg[1];
			voiceGroup.set(\timbre, timbre);
		});

		this.addCommand(\oscWaveShape, "ii", {
			arg msg;
			this.setArgOnVoice(msg[1], \oscWaveShape, msg[2]);
		});

		this.addCommand(\oscWaveShapeAll, "i", {
			arg msg;
			oscWaveShape = msg[1];
			voiceGroup.set(\oscWaveShape, oscWaveShape);
		});

		this.addCommand(\pulseWidth, "if", {
			arg msg;
			this.setArgOnVoice(msg[1], \pulseWidth, msg[2]);
		});

		this.addCommand(\pulseWidthAll, "f", {
			arg msg;
			pulseWidth = msg[1];
			voiceGroup.set(\pulseWidth, pulseWidth);
		});

		this.addCommand(\pulseWidthModEnv, "if", {
			arg msg;
			this.setArgOnVoice(msg[1], \pulseWidthModEnv, msg[2]);
		});

		this.addCommand(\pulseWidthModEnvAll, "f", {
			arg msg;
			pulseWidthModEnv = msg[1];
			voiceGroup.set(\pulseWidthModEnv, pulseWidthModEnv);
		});

		this.addCommand(\pulseWidthModLfo, "if", {
			arg msg;
			this.setArgOnVoice(msg[1], \pulseWidthModLfo, msg[2]);
		});

		this.addCommand(\pulseWidthModLfoAll, "f", {
			arg msg;
			pulseWidthModLfo = msg[1];
			voiceGroup.set(\pulseWidthModLfo, pulseWidthModLfo);
		});

		this.addCommand(\pulseWidthModPressure, "if", {
			arg msg;
			this.setArgOnVoice(msg[1], \pulseWidthModPressure, msg[2]);
		});

		this.addCommand(\pulseWidthModPressureAll, "f", {
			arg msg;
			pulseWidthModPressure = msg[1];
			voiceGroup.set(\pulseWidthModPressure, \pulseWidthModPressure);
		});

		this.addCommand(\pulseWidthModTimbre, "if", {
			arg msg;
			this.setArgOnVoice(msg[1], \pulseWidthModTimbre, msg[2]);
		});

		this.addCommand(\pulseWidthModTimbreAll, "f", {
			arg msg;
			pulseWidthModTimbre = msg[1];
			voiceGroup.set(\pulseWidthModTimbre, pulseWidthModTimbre);
		});

		this.addCommand(\freqModLfo, "if", {
			arg msg;
			this.setArgOnVoice(msg[1], \freqModLfo, msg[2]);
		});

		this.addCommand(\freqModLfoAll, "f", {
			arg msg;
			freqModLfo = msg[1];
			voiceGroup.set(\freqModLfo, freqModLfo);
		});

		this.addCommand(\freqModEnv, "if", {
			arg msg;
			this.setArgOnVoice(msg[1], \freqModEnv, msg[2]);
		});

		this.addCommand(\freqModEnvAll, "f", {
			arg msg;
			freqModEnv = msg[1];
			voiceGroup.set(\freqModEnv, freqModEnv);
		});

		this.addCommand(\glideAll, "f", {
			arg msg;
			glide = msg[1];
			voiceGroup.set(\glide, glide);
		});

		this.addCommand(\mainOscLevel, "if", {
			arg msg;
			this.setArgOnVoice(msg[1], \mainOscLevel, msg[2]);
		});

		this.addCommand(\mainOscLevelAll, "f", {
			arg msg;
			mainOscLevel = msg[1];
			voiceGroup.set(\mainOscLevel, mainOscLevel);
		});

		this.addCommand(\mainOscLevelModEnv, "if", {
			arg msg;
			this.setArgOnVoice(msg[1], \mainOscLevelModEnv, msg[2]);
		});

		this.addCommand(\mainOscLevelModEnvAll, "f", {
			arg msg;
			mainOscLevelModEnv = msg[1];
			voiceGroup.set(\mainOscLevelModEnv, mainOscLevelModEnv);
		});

		this.addCommand(\mainOscLevelModLfo, "if", {
			arg msg;
			this.setArgOnVoice(msg[1], \mainOscLevelModLfo, msg[2]);
		});

		this.addCommand(\mainOscLevelModLfoAll, "f", {
			arg msg;
			mainOscLevelModLfo = msg[1];
			voiceGroup.set(\mainOscLevelModLfo, mainOscLevelModLfo);
		});

		this.addCommand(\mainOscLevelModPressure, "if", {
			arg msg;
			this.setArgOnVoice(msg[1], \mainOscLevelModPressure, msg[2]);
		});

		this.addCommand(\mainOscLevelModPressureAll, "f", {
			arg msg;
			mainOscLevelModPressure = msg[1];
			voiceGroup.set(\mainOscLevelModPressure, mainOscLevelModPressure);
		});

		this.addCommand(\mainOscLevelModTimbre, "if", {
			arg msg;
			this.setArgOnVoice(msg[1], \mainOscLevelModTimbre, msg[2]);
		});

		this.addCommand(\mainOscLevelModTimbreAll, "f", {
			arg msg;
			mainOscLevelModTimbre = msg[1];
			voiceGroup.set(\mainOscLevelModTimbre, mainOscLevelModTimbre);
		});

		this.addCommand(\subOscLevel, "if", {
			arg msg;
			this.setArgOnVoice(msg[1], \subOscLevel, msg[2]);
		});

		this.addCommand(\subOscLevelAll, "f", {
			arg msg;
			subOscLevel = msg[1];
			voiceGroup.set(\subOscLevel, subOscLevel);
		});

		this.addCommand(\subOscLevelModEnv, "if", {
			arg msg;
			this.setArgOnVoice(msg[1], \subOscLevelModEnv, msg[2]);
		});

		this.addCommand(\subOscLevelModEnvAll, "f", {
			arg msg;
			subOscLevelModEnv = msg[1];
			voiceGroup.set(\subOscLevelModEnv, subOscLevelModEnv);
		});

		this.addCommand(\subOscLevelModLfo, "if", {
			arg msg;
			this.setArgOnVoice(msg[1], \subOscLevelModLfo, msg[2]);
		});

		this.addCommand(\subOscLevelModLfoAll, "f", {
			arg msg;
			subOscLevelModLfo = msg[1];
			voiceGroup.set(\subOscLevelModLfo, subOscLevelModLfo);
		});

		this.addCommand(\subOscLevelModPressure, "if", {
			arg msg;
			this.setArgOnVoice(msg[1], \subOscLevelModPressure, msg[2]);
		});

		this.addCommand(\subOscLevelModPressureAll, "f", {
			arg msg;
			subOscLevelModPressure = msg[1];
			voiceGroup.set(\subOscLevelModPressure, subOscLevelModPressure);
		});

		this.addCommand(\subOscLevelModTimbre, "if", {
			arg msg;
			this.setArgOnVoice(msg[1], \subOscLevelModTimbre, msg[2]);
		});

		this.addCommand(\subOscLevelModTimbreAll, "f", {
			arg msg;
			subOscLevelModTimbre = msg[1];
			voiceGroup.set(\subOscLevelModTimbre, subOscLevelModTimbre);
		});

		this.addCommand(\subOscDetune, "if", {
			arg msg;
			this.setArgOnVoice(msg[1], \subOscDetune, msg[2]);
		});

		this.addCommand(\subOscDetuneAll, "f", {
			arg msg;
			subOscDetune = msg[1];
			voiceGroup.set(\subOscDetune, subOscDetune);
		});

		this.addCommand(\noiseLevel, "if", {
			arg msg;
			this.setArgOnVoice(msg[1], \noiseLevel, msg[2]);
		});

		this.addCommand(\noiseLevelAll, "f", {
			arg msg;
			noiseLevel = msg[1];
			voiceGroup.set(\noiseLevel, noiseLevel);
		});

		this.addCommand(\noiseLevelModEnv, "if", {
			arg msg;
			this.setArgOnVoice(msg[1], \noiseLevelModEnv, msg[2]);
		});

		this.addCommand(\noiseLevelModEnvAll, "f", {
			arg msg;
			noiseLevelModEnv = msg[1];
			voiceGroup.set(\noiseLevelModEnv, noiseLevelModEnv);
		});

		this.addCommand(\noiseLevelModLfo, "if", {
			arg msg;
			this.setArgOnVoice(msg[1], \noiseLevelModLfo, msg[2]);
		});

		this.addCommand(\noiseLevelModLfoAll, "f", {
			arg msg;
			noiseLevelModLfo = msg[1];
			voiceGroup.set(\noiseLevelModLfo, noiseLevelModLfo);
		});

		this.addCommand(\noiseLevelModPressure, "if", {
			arg msg;
			this.setArgOnVoice(msg[1], \noiseLevelModPressure, msg[2]);
		});

		this.addCommand(\noiseLevelModPressureAll, "f", {
			arg msg;
			noiseLevelModPressure = msg[1];
			voiceGroup.set(\noiseLevelModPressure, noiseLevelModPressure);
		});

		this.addCommand(\noiseLevelModTimbre, "if", {
			arg msg;
			this.setArgOnVoice(msg[1], \noiseLevelModTimbre, msg[2]);
		});

		this.addCommand(\noiseLevelModTimbreAll, "f", {
			arg msg;
			noiseLevelModTimbre = msg[1];
			voiceGroup.set(\noiseLevelModTimbre, noiseLevelModTimbre);
		});

		this.addCommand(\hpFilterCutoff, "if", {
			arg msg;
			this.setArgOnVoice(msg[1], \hpFilterCutoff, msg[2]);
		});

		this.addCommand(\hpFilterCutoffAll, "f", {
			arg msg;
			hpFilterCutoff = msg[1];
			voiceGroup.set(\hpFilterCutoff, hpFilterCutoff);
		});

		this.addCommand(\lpFilterType, "ii", {
			arg msg;
			this.setArgOnVoice(msg[1], \lpFilterType, msg[2]);
		});

		this.addCommand(\lpFilterTypeAll, "i", {
			arg msg;
			lpFilterType = msg[1];
			voiceGroup.set(\lpFilterType, lpFilterType);
		});

		this.addCommand(\lpFilterCutoff, "if", {
			arg msg;
			this.setArgOnVoice(msg[1], \lpFilterCutoff, msg[2]);
		});

		this.addCommand(\lpFilterCutoffAll, "f", {
			arg msg;
			lpFilterCutoff = msg[1];
			voiceGroup.set(\lpFilterCutoff, lpFilterCutoff);
		});

		this.addCommand(\lpFilterCutoffEnvSelect, "ii", {
			arg msg;
			this.setArgOnVoice(msg[1], \lpFilterCutoffEnvSelect, msg[2]);
		});

		this.addCommand(\lpFilterCutoffEnvSelectAll, "i", {
			arg msg;
			lpFilterCutoffEnvSelect = msg[1];
			voiceGroup.set(\lpFilterCutoffEnvSelect, lpFilterCutoffEnvSelect);
		});

		this.addCommand(\lpFilterCutoffModEnv, "if", {
			arg msg;
			this.setArgOnVoice(msg[1], \lpFilterCutoffModEnv, msg[2]);
		});

		this.addCommand(\lpFilterCutoffModEnvAll, "f", {
			arg msg;
			lpFilterCutoffModEnv = msg[1];
			voiceGroup.set(\lpFilterCutoffModEnv, lpFilterCutoffModEnv);
		});

		this.addCommand(\lpFilterCutoffModLfo, "if", {
			arg msg;
			this.setArgOnVoice(msg[1], \lpFilterCutoffModLfo, msg[2]);
		});

		this.addCommand(\lpFilterCutoffModLfoAll, "f", {
			arg msg;
			lpFilterCutoffModLfo = msg[1];
			voiceGroup.set(\lpFilterCutoffModLfo, lpFilterCutoffModLfo);
		});

		this.addCommand(\lpFilterCutoffModPressure, "if", {
			arg msg;
			this.setArgOnVoice(msg[1], \lpFilterCutoffModPressure, msg[2]);
		});

		this.addCommand(\lpFilterCutoffModPressureAll, "f", {
			arg msg;
			lpFilterCutoffModPressure = msg[1];
			voiceGroup.set(\lpFilterCutoffModPressure, lpFilterCutoffModPressure);
		});

		this.addCommand(\lpFilterCutoffModTimbre, "if", {
			arg msg;
			this.setArgOnVoice(msg[1], \lpFilterCutoffModTimbre, msg[2]);
		});

		this.addCommand(\lpFilterCutoffModTimbreAll, "f", {
			arg msg;
			lpFilterCutoffModTimbre = msg[1];
			voiceGroup.set(\lpFilterCutoffModTimbre, lpFilterCutoffModTimbre);
		});

		this.addCommand(\lpFilterResonance, "if", {
			arg msg;
			this.setArgOnVoice(msg[1], \lpFilterResonance, msg[2]);
		});

		this.addCommand(\lpFilterResonanceAll, "f", {
			arg msg;
			lpFilterResonance = msg[1];
			voiceGroup.set(\lpFilterResonance, lpFilterResonance);
		});

		this.addCommand(\lpFilterTracking, "if", {
			arg msg;
			this.setArgOnVoice(msg[1], \lpFilterTracking, msg[2]);
		});

		this.addCommand(\lpFilterTrackingAll, "f", {
			arg msg;
			lpFilterTracking = msg[1];
			voiceGroup.set(\lpFilterTracking, lpFilterTracking);
		});

		this.addCommand(\lfoFade, "if", {
			arg msg;
			this.setArgOnVoice(msg[1], \lfoFade, msg[2]);
		});

		this.addCommand(\lfoFadeAll, "f", {
			arg msg;
			lfoFade = msg[1];
			voiceGroup.set(\lfoFade, lfoFade);
		});

		this.addCommand(\env1AttackAll, "f", {
			arg msg;
			env1Attack = msg[1];
			voiceGroup.set(\env1Attack, env1Attack);
		});

		this.addCommand(\env1Decay, "if", {
			arg msg;
			this.setArgOnVoice(msg[1], \env1Decay, msg[2]);
		});

		this.addCommand(\env1DecayAll, "f", {
			arg msg;
			env1Decay = msg[1];
			voiceGroup.set(\env1Decay, env1Decay);
		});

		this.addCommand(\env1Sustain, "if", {
			arg msg;
			this.setArgOnVoice(msg[1], \env1Sustain, msg[2]);
		});

		this.addCommand(\env1SustainAll, "f", {
			arg msg;
			env1Sustain = msg[1];
			voiceGroup.set(\env1Sustain, env1Sustain);
		});

		this.addCommand(\env1Release, "if", {
			arg msg;
			this.setArgOnVoice(msg[1], \env1Release, msg[2]);
		});

		this.addCommand(\env1ReleaseAll, "f", {
			arg msg;
			env1Release = msg[1];
			voiceGroup.set(\env1Release, env1Release);
		});

		this.addCommand(\env2AttackAll, "f", {
			arg msg;
			env2Attack = msg[1];
			voiceGroup.set(\env2Attack, env2Attack);
		});

		this.addCommand(\env2Decay, "if", {
			arg msg;
			this.setArgOnVoice(msg[1], \env2Decay, msg[2]);
		});

		this.addCommand(\env2DecayAll, "f", {
			arg msg;
			env2Decay = msg[1];
			voiceGroup.set(\env2Decay, env2Decay);
		});

		this.addCommand(\env2Sustain, "if", {
			arg msg;
			this.setArgOnVoice(msg[1], \env2Sustain, msg[2]);
		});

		this.addCommand(\env2SustainAll, "f", {
			arg msg;
			env2Sustain = msg[1];
			voiceGroup.set(\env2Sustain, env2Sustain);
		});

		this.addCommand(\env2Release, "if", {
			arg msg;
			this.setArgOnVoice(msg[1], \env2Release, msg[2]);
		});

		this.addCommand(\env2ReleaseAll, "f", {
			arg msg;
			env2Release = msg[1];
			voiceGroup.set(\env2Release, env2Release);
		});

		this.addCommand(\amp, "if", {
			arg msg;
			this.setArgOnVoice(msg[1], \amp, msg[2]);
		});

		this.addCommand(\ampAll, "f", {
			arg msg;
			amp = msg[1];
			voiceGroup.set(\amp, amp);
		});

		this.addCommand(\ampModLfo, "if", {
			arg msg;
			this.setArgOnVoice(msg[1], \ampModLfo, msg[2]);
		});

		this.addCommand(\ampModLfoAll, "f", {
			arg msg;
			ampModLfo = msg[1];
			voiceGroup.set(\ampModLfo, ampModLfo);
		});

		this.addCommand(\ampModPressure, "if", {
			arg msg;
			this.setArgOnVoice(msg[1], \ampModPressure, msg[2]);
		});

		this.addCommand(\ampModPressureAll, "f", {
			arg msg;
			ampModPressure = msg[1];
			voiceGroup.set(\ampModPressure, ampModPressure);
		});

		this.addCommand(\ampModTimbre, "if", {
			arg msg;
			this.setArgOnVoice(msg[1], \ampModTimbre, msg[2]);
		});

		this.addCommand(\ampModTimbreAll, "f", {
			arg msg;
			ampModTimbre = msg[1];
			voiceGroup.set(\ampModTimbre, ampModTimbre);
		});

		this.addCommand(\ringModFade, "if", {
			arg msg;
			this.setArgOnVoice(msg[1], \ringModFade, msg[2]);
		});

		this.addCommand(\ringModFadeAll, "f", {
			arg msg;
			ringModFade = msg[1];
			voiceGroup.set(\ringModFade, ringModFade);
		});

		this.addCommand(\ringModMix, "if", {
			arg msg;
			this.setArgOnVoice(msg[1], \ringModMix, msg[2]);
		});

		this.addCommand(\ringModMixAll, "f", {
			arg msg;
			ringModMix = msg[1];
			voiceGroup.set(\ringModMix, ringModMix);
		});

		this.addCommand(\ringModMixModEnv, "if", {
			arg msg;
			this.setArgOnVoice(msg[1], \ringModMixModEnv, msg[2]);
		});

		this.addCommand(\ringModMixModEnvAll, "f", {
			arg msg;
			ringModMixModEnv = msg[1];
			voiceGroup.set(\ringModMixModEnv, ringModMixModEnv);
		});

		this.addCommand(\ringModMixModLfo, "if", {
			arg msg;
			this.setArgOnVoice(msg[1], \ringModMixModLfo, msg[2]);
		});

		this.addCommand(\ringModMixModLfoAll, "f", {
			arg msg;
			ringModMixModLfo = msg[1];
			voiceGroup.set(\ringModMixModLfo, ringModMixModLfo);
		});

		this.addCommand(\ringModMixModPressure, "if", {
			arg msg;
			this.setArgOnVoice(msg[1], \ringModMixModPressure, msg[2]);
		});

		this.addCommand(\ringModMixModPressureAll, "f", {
			arg msg;
			ringModMixModPressure = msg[1];
			voiceGroup.set(\ringModMixModPressure, ringModMixModPressure);
		});

		this.addCommand(\ringModMixModTimbre, "if", {
			arg msg;
			this.setArgOnVoice(msg[1], \ringModMixModTimbre, msg[2]);
		});

		this.addCommand(\ringModMixModTimbreAll, "f", {
			arg msg;
			ringModMixModTimbre = msg[1];
			voiceGroup.set(\ringModMixModTimbre, ringModMixModTimbre);
		});

		this.addCommand(\chorusMix, "f", {
			arg msg;
			mixer.set(\chorusMix, msg[1]);
		});

		this.addCommand(\lfoFreq, "f", {
			arg msg;
			lfo.set(\lfoFreq, msg[1]);
		});

		this.addCommand(\lfoWaveShape, "i", {
			arg msg;
			lfo.set(\lfoWaveShape, msg[1]);
		});

		this.addCommand(\ringModFreq, "f", {
			arg msg;
			lfo.set(\ringModFreq, msg[1]);
		});

	}

	free {
		voiceGroup.free;
		lfo.free;
		mixer.free;
	}
}
