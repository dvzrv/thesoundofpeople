SNPSonificator {
	var <>numChannels;//number of channels to use
	var <snpSynths;
	var <>snpDict;//the SNPDict to use
	var <>playTime;//calculated play time of a single SNP, defined by playTime setup on init
	var <>includeUnknown;//wether to include SNPs with unknown resolvers (aka. SNPS with only bases set)
	var <>ignoreSNPs = #[];//Array of Symbols of chromosomes to ignore (1-22,X,Y,MT)
	var <keys;//Array storing normalized settings for each chromosome
	var <keyToFrq;//Dictionary with normalized frequency ranges for 8 different chromosome lengths
	var <play = false;//boolean indicating the state of playing
	var <currentStep = 0.0;//storage for the position up to which sonification should take place
	var <lastStep = 0.0;//storage for the last played step (SNP position) aka. current position playing
	var <lengthOfSNP = 1.0;//distance between two SNPs in ms
	var <speakerSetup;//Array holding the chromosome numbers for each speaker
	var <synthGroup;//Group for the Synths
	var <fxGroup;//Group for effects
	var <compressionGroup;//Group for compression Synths
	var <recordingGroup;//Group for recording Synths
	var <sonBus;//Bus for Synth->compression
	var <fxBus;//Bus for chromosome based effects
	var <recBus;//Bus for compression->recording/out
	var <record =false;//boolean indicating wether to record or not (default off)
	var <>recPath;//String representing directory to use for recording
	var <recordingBuffer;//the recording buffer to use, wehen recording
	var <baseTone = #[110.00, 65.4, 98.00, 73.42];//the fundamentals from which 8 octaves will be calculated: A (A2), C (C2), G (G2), T (D2)

	*new{
		arg snpDict, numChannels = 2, playTime, includeUnknown = false, ignoreSNPs, setRecord = false, recordPath;
		^super.new.sonificatorInit(snpDict, numChannels, playTime, includeUnknown, ignoreSNPs, setRecord, recordPath);
	}

	sonificatorInit{
		arg snpDict, numChannels, playTime, includeUnknown, ignoreSNPs, setRecord, recordPath;
		this.snpDict = snpDict;
		this.numChannels_(numChannels);
		this.playTime = playTime;
		lengthOfSNP = this.calcSNPTime(playTime);
		this.includeUnknown = includeUnknown;
		if(ignoreSNPs.notNil && ignoreSNPs.isArray,{
			this.ignoreSNPs = ignoreSNPs;
		});
		this.initKeyRange.value;
		this.setupSpeakers(numChannels, true);
		this.setupGroups(numChannels);
		this.setupBusses(numChannels);
		this.addSNPSynths.value;
		//TODO: add effects Synths based on chromosome areas
		if(setRecord,{//if recording should be set up
			if(recordPath.notNil && recordPath.isString,{//first check if recordingPath is set
				if(recordPath[recordPath.size-1]!="/",{//compensate for missing trailing slash
					recPath = (recordPath++"/");
				},{
					recPath = recordPath;
				});
				if(File.exists(recPath),{//if path exists, init recording to file
					recPath = recordPath;
					record = setRecord;
					this.setupRecording(numChannels);	
				},{
					("Path does not exist: "++recPath.asString).postln;
					("Update recPath!")
				});
			},{
				("Recording path invalid: "++recPath.asString).postln;
			});
		});
	}

	setupBusses{//setup the busses for compression and recording
		arg channels;
		sonBus = Bus.audio(Server.local, channels);
		recBus = Bus.audio(Server.local, channels);
	}
	
	setupGroups{//setup Groups (for Synths, compression and recording)
		arg channels;
		synthGroup = Group.new;
//		fxGroup = Group.after(synthGroup);
		compressionGroup = Group.after(synthGroup);
		recordingGroup = Group.after(compressionGroup);
		NodeWatcher.register(synthGroup);
//		NodeWatcher.register(fxGroup);
		NodeWatcher.register(compressionGroup);
		NodeWatcher.register(recordingGroup);
	}
	
	setupRecording{//create a buffer and a recording SynthDef, leaving file open for writing
		arg channels;
		("Setting up recording to file.").postln;
		recordingBuffer = Buffer.alloc(Server.local, 262144, channels);
		recordingBuffer.write(recPath++"SNPSonification-"++(Date.seed).asString++"_("++channels.asString++"_chan).aiff".standardizePath, "aiff", "float32", 0, 0, true);
		SynthDef(\recordSNP,{
			arg buffer;
			DiskOut.ar(buffer, In.ar(recBus, channels));
		}).add;
	}
	
	setupSpeakers{//setup speakers for stereo and circular multichannel
		arg channels, printOuts = false;
		switch(channels.asInt, 
			2,{speakerSetup = [[8,16,24,6,14,22,4,12,20,2,10,18],[7,15,23,1,9,17,3,11,19,5,11,21]];},
			4,{speakerSetup = [[3,7,11,15,19,23],[4,8,12,16,20,24],[2,6,10,14,18,22],[1,5,9,13,17,21]];},
			6,{speakerSetup = [[5,11,17,23],[6,12,18,24],[4,10,16,12],[2,8,14,20],[1,7,13,19],[3,9,15,21]];},
			8,{speakerSetup = [[7,15,23],[8,16,24],[6,14,22],[4,12,20],[2,10,18],[1,9,17],[3,11,19],[5,13,21]];},
			{"Invalid number of speakers set! Fix this by calling setupSpeakers(nSpeakers) on this object or this will break. Seriously!".postln;}
		);
		if(printOuts,{//print the speaker setup
			speakerSetup.do({
				arg item, i;
				("Speaker "++(i+1).asString++" has chromosomes: ").post; 
				item.do({
					arg chromosome;
					(chromosome.asString++" ("++keys[chromosome-1].asString++") ").post;
				});
				"".postln;
			});
		});
	}

	initKeyRange{//init 8 octave spanning frequency set, map SNPs to frequencies
		keys = round(SNPInfo.chromosomesFactor[0..SNPInfo.chromosomesFactor.size-2].normalizeSum*10, 0.125);
		keys.add(0.0);
		keyToFrq = Dictionary.new(8);
		8.do({//setup for 8 octaves
			arg item, i;
			switch(i,
				0, {keyToFrq.put(0.000, this.calcFrqCombination(baseTone));},
				1, {keyToFrq.put(0.125, this.calcFrqCombination(baseTone*2.pow(i)));},
				2, {keyToFrq.put(0.250, this.calcFrqCombination(baseTone*2.pow(i)));},
				3, {keyToFrq.put(0.375, this.calcFrqCombination(baseTone*2.pow(i)));},
				4, {keyToFrq.put(0.500, this.calcFrqCombination(baseTone*2.pow(i)));},
				5, {keyToFrq.put(0.625, this.calcFrqCombination(baseTone*2.pow(i)));},
				6, {keyToFrq.put(0.750, this.calcFrqCombination(baseTone*2.pow(i)));},
				7, {keyToFrq.put(0.875, this.calcFrqCombination(baseTone*2.pow(i)));}
			);
		});
	}
	
	calcFrqCombination{//calculate tones for bases and their combinations for a given set of notes from the same octave
		arg baseTones;
		^Dictionary.newFrom(List[
			SNPInfo.a, baseTones[0], 
			SNPInfo.c, baseTones[1], 
			SNPInfo.g, baseTones[2], 
			SNPInfo.t, baseTones[3], 
			SNPInfo.a-SNPInfo.c, (baseTones[0]+baseTones[1])/2, 
			SNPInfo.a-SNPInfo.g, (baseTones[0]+baseTones[2])/2, 
			SNPInfo.a-SNPInfo.t, (baseTones[0]+baseTones[3])/2, 
			SNPInfo.c-SNPInfo.g, (baseTones[1]+baseTones[2])/2, 
			SNPInfo.c-SNPInfo.t, (baseTones[1]+baseTones[3])/2, 
			SNPInfo.g-SNPInfo.t, (baseTones[2]+baseTones[3])/2]
		);
	}
	
	addSNPSynths{//Synthdefs for base pairs, single bases and the MT SNPs
		"Adding SynthDef for base pairs -> \base_pair".postln;
		SynthDef(\base_pair, {|out=0, freq=#[0.0,0.0,0.0] ,amplitude=0.6, attackTime = 0.01, releaseTime = 0.3, curve = -4|	
		var env = EnvGen.kr(
				Env.perc(attackTime,releaseTime,amplitude,curve),doneAction:2//free the enclosing Synth when done
			);
		Out.ar(
			sonBus.index+out,//speaker to play on
			SinOsc.ar(//sine tone for the base pair
				freq[0],0,env,
				SinOsc.ar(//sine tone for the first (lower) resolver
					freq[1],0,env*0.5
				)+
				SinOsc.ar(//sine tone for the second (upper) resolver
					freq[2],0,env * 0.5
				)
			)
		);}, variants: (//variants for 23 first chromosomes
			1: [amplitude: 0.10, attackTime:0.01, releaseTime:0.7, curve:-4],//0.125
			2: [amplitude: 0.10, attackTime:0.01, releaseTime:0.7, curve:-4],//0.125
			3: [amplitude: 0.08, attackTime:0.01, releaseTime:0.6, curve:-4],//0.250
			4: [amplitude: 0.08, attackTime:0.01, releaseTime:0.6, curve:-4],//0.250
			5: [amplitude: 0.08, attackTime:0.01, releaseTime:0.6, curve:-4],//0.250
			6: [amplitude: 0.08, attackTime:0.01, releaseTime:0.6, curve:-4],//0.250
			7: [amplitude: 0.08, attackTime:0.01, releaseTime:0.6, curve:-4],//0.250
			8: [amplitude: 0.08, attackTime:0.01, releaseTime:0.6, curve:-4],//0.250
			9: [amplitude: 0.08, attackTime:0.01, releaseTime:0.6, curve:-4],//0.250
			10: [amplitude: 0.06, attackTime:0.01, releaseTime:0.5, curve:-2],//0.375
			11: [amplitude: 0.06, attackTime:0.01, releaseTime:0.5, curve:-2],//0.375
			12: [amplitude: 0.06, attackTime:0.01, releaseTime:0.5, curve:-2],//0.375
			13: [amplitude: 0.06, attackTime:0.01, releaseTime:0.5, curve:-2],//0.375
			14: [amplitude: 0.06, attackTime:0.01, releaseTime:0.5, curve:-2],//0.375
			15: [amplitude: 0.06, attackTime:0.01, releaseTime:0.5, curve:-2],//0.375
			16: [amplitude: 0.04, attackTime:0.01, releaseTime:0.5, curve:-2],//0.500
			17: [amplitude: 0.04, attackTime:0.01, releaseTime:0.5, curve:-2],//0.500
			18: [amplitude: 0.04, attackTime:0.01, releaseTime:0.5, curve:-2],//0.500
			19: [amplitude: 0.04, attackTime:0.01, releaseTime:0.5, curve:-2],//0.750
			20: [amplitude: 0.05, attackTime:0.01, releaseTime:0.5, curve:-2],//0.625
			21: [amplitude: 0.05, attackTime:0.01, releaseTime:0.5, curve:-2],//0.875
			22: [amplitude: 0.05, attackTime:0.01, releaseTime:0.5, curve:-2],//0.875
			23: [amplitude: 0.05, attackTime:0.01, releaseTime:0.5, curve:-2]//0.875
		)).add;
		"Adding SynthDef for single bases -> \base_single".postln;
		SynthDef(\base_single, {|out=0, freq=#[0.0,0.0] ,amplitude=0.6, attackTime = 0.01, releaseTime = 0.3, curve = -4|
			var env = EnvGen.kr(
				Env.perc(attackTime,releaseTime,amplitude,curve),doneAction:2//free the enclosing Synth when done
			);
			Out.ar(
				sonBus.index+out,
				Saw.ar(//sine tone for the base
					freq[0],env,SinOsc.ar(//sine tone for the resolver
						freq[1],0,env*0.5
					)
				)
			);},variants: (//variants for X and Y chromosomes
				23:[amplitude: 0.07, attackTime: 0.01, releaseTime: 0.5, curve: -4],//0.250 -> X
				24:[amplitude: 0.10, attackTime: 0.01, releaseTime: 0.7, curve: -4]//0.75 ->Y
		)).add;
		"Adding SynthDef for MT bases -> \base_mt".postln;
		SynthDef(\base_mt, {|startPosition = 0, freq = #[0.0,0.0], attackTime = 0.01, releaseTime = 1.5, amplitude = 0.4, curve = -4 |
			var mtSynth, panAround; 
			var env = EnvGen.kr(
				Env.perc(attackTime,releaseTime,amplitude,curve),doneAction:2//free the enclosing Synth when done
			);
			mtSynth = Saw.ar(//sine tone for the base
				freq[0],env,SinOsc.ar(//sine tone for the resolver
					freq[1],0,env*0.5
				)
			);
			panAround = PanAz.ar(
				numChannels,//span the whole number of channels available
				mtSynth,//use the MT Synth as input
				LFSaw.kr(2,0),//pan around TODO: + startPosition
				0.5,//control level
				2,//width of sound
				startPosition//random starting point
			);
			Out.ar(sonBus, panAround);
		}).add;
		"Adding SynthDef for compression -> \compressor".postln;
		SynthDef(\compressor, {|inBus|
			var in,compressed;
			in = In.ar(inBus,numChannels);
			compressed = Compander.ar(
				in,//signal in
				in,//control signal
				0.5,//threshold
				1,//slopeBelow
				0.3,//slopeAbove
				0.002,//clampTime
				0.01//relaxTime
			);
			ReplaceOut.ar(0, compressed);
			Out.ar(recBus, compressed);
		}).add;
	}
	
	startDSPSynths{//start compression and possibly recording Synths
		("Starting compression Synth.").postln;
		Synth(\compressor, [inBus:sonBus], compressionGroup);//play compression Synth
		if(record,{//if recording, play recording Synth
			("Starting recording Synth for recording to file.").postln;
			Synth(\recordSNP, [buffer: this.recordingBuffer], recordingGroup);
		});
	}

	stopDSPSynths{//stop compression and possibly recording Synths
		Routine{
			2.wait;
			if(record,{
				("Stopping recording to file.").postln;
				recordingBuffer.close;
				recordingBuffer.free;
				if(recordingGroup.isRunning,{
					recordingGroup.run(false);
				});
			});
			1.wait;
			if(compressionGroup.isRunning,{
				("Stopping compressor Synth.").postln;
				compressionGroup.run(false);
			});
		}.play;
	}
	
	playFromTo{//start from a position (if none set, start from the beginning), play to a position (or end, if none set)
		arg fromPosition = 0.0, toPosition = snpDict.positions;//start from position, play to position
		var startTime=0.0;//time to first position (if not playing from the beginning, this stays the same)
		this.startDSPSynths;//start Synths for compression and recording
		play = true; //set play parameter
		if((fromPosition == 0.0) || (snpDict.positions<fromPosition),{//if playing from start (or out of bounds), calculate start time for the first SNP
			startTime = lengthOfSNP*snpDict.distanceToNextPosition(fromPosition.asFloat);
			currentStep = fromPosition;//set start position
		},{//else start at once 
			currentStep = fromPosition;
		});
		if((toPosition>snpDict.positions) || (toPosition < fromPosition) || (toPosition.isNil),{//if to position is out of bounds or smaller then from, set to total length
			lastStep = snpDict.positions;
		},{//else play up to that position
			lastStep = toPosition;
		});
		("Starting to play positions: "++currentStep++" - "++lastStep).postln;
		SystemClock.sched(startTime,{//start a SystemClock immediately or with calculated startTime to read from the SNPDict
			arg time;
			var resched = 0.0;
			if((play) && (snpDict.positions>currentStep) && (currentStep <= lastStep),{//if playing and positions are left, play the position and reschedule SystemClock to the next SNP
//				("Playing SNP #"++currentStep++" of "++snpDict.positions++".").postln;
				resched = lengthOfSNP*this.playPosition(snpDict.snpAtPosition(snpDict.snpArr[currentStep]), snpDict.snpArr[currentStep]);
				currentStep = currentStep+1.0;
//				("Rescheduling in "++resched++"s for step #"++currentStep++" of "++snpDict.positions).postln;
				resched;
			},{//end the SystemClock and print out last position
				("Done playing. Last position: "++(currentStep-1).asString).postln;
				this.stopDSPSynths;
				nil;
			});
		});
	}
	
	pausePlay{//pause the sonification
		play = false;
	}
	
	resumePlay{//resume the sonification from last position played
		this.playSon(currentStep, lastStep);
	}
	
	playPosition{//play the SNP(s) at a position and return the distance to the next
		arg posDict, position;
		posDict.keysValuesDo{
			arg key, value;
			if(ignoreSNPs.includes(key).not,{//ignore chromosomes if that is set up
				if(includeUnknown,{//if SNPs without resolvers should be ignored, do that
					this.playBase(value);
				},{
					if(value.hasResolver,{
						this.playBase(value);
					});
				});
			});
		};
		^snpDict.distanceToNextPosition(position);
	}

	playBase{//play the actual base/base pair
		arg snp;
		var baseKey = keyToFrq.at(keys[snp.vecChromosome-1]);
		if(SNPInfo.isBasePair(snp.base),{//play base pairs
			Synth('base_pair.'++snp.vecChromosome.asSymbol, [out: this.findSpeaker(snp.vecChromosome), freq: [baseKey.at(snp.vecBase), baseKey.at(snp.vecResolver[0]), baseKey.at(snp.vecResolver[1])]], synthGroup);
		},{
			if(SNPInfo.isBase(snp.base), {//play single bases
				if(snp.chromosome==\MT, {//play the MT chromosome
					Synth('base_mt', [startPosition: rrand(numChannels,0), freq: [baseKey.at(snp.vecBase), baseKey.at(snp.vecResolver[0])]], synthGroup);
				},{//play parts of the X and the Y chromosome
					Synth('base_single.'++snp.vecChromosome.asSymbol, [out: this.findSpeaker(snp.vecChromosome), freq: [baseKey.at(snp.vecBase), baseKey.at(snp.vecResolver[0])]], synthGroup);
				});
			});
		});
		baseKey.free;
	}

	findSpeaker{//find the speaker for the chromosome (defined in speakerSetup)
		arg chromosome;
		var speaker = 0;
		speakerSetup.do({
			arg item, i;
			if(item.includes(chromosome),{
				speaker = i;
			});
		});
		^speaker;
	}

	calcSNPTime{//calculate the clock time for a single SNP
		arg playTime;
		var snpTime;
		snpTime = ((1/SNPInfo.lengthAtOneMs)*playTime)/1000;
		^snpTime;
	}

	queryTimeAndPosition{//query current time and position
		("Positions: "++currentStep.asString++" - "++lastStep.asString++".").postln;
		("Play time: "++(currentStep*lengthOfSNP*1000).asString++"s - "++(lastStep*lengthOfSNP*1000).asString++"s.").postln;
	}
}
