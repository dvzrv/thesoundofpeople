SNPInfo{//helper class for calculations and constants used in the other classes
	const <baseSingle = #[\A, \C, \G, \T];
	const <basePair = #[\AA, \AC, \AG, \AT, \CC, \CG, \CT, \GG, \GT, \TT];
	const <unknownBasePair = #[\AA, \CC, \GG, \TT];
	const <emptyBasePair = #[\none, \none];
	const <emptyBase = #[\none];
//	const <chromosomesLength = #[249250621, 243199373, 198022430, 191154276, 180915260, 171115067, 159138663, 146364022, 141213431, 135534747, 135006516, 133851895, 115169878, 107349540, 102531392, 90354753, 81195210, 78077248, 59128983, 63025520, 48129895, 51304566, 155270560, 59373566, 16569];
	const <chromosomesLength = #[247199719, 242751149, 199446827, 191263063, 180837866, 170896993, 158821424, 146274826, 140442298, 135374737, 134452384, 132289534, 114127980, 106360585, 100338915, 88822254, 78654742, 76117153, 63806651, 62435965, 46944323, 49528953, 154913754, 57741652, 16569];
	const <chromosomesFactor = #[ 1, 1.0183256393155, 1.2394266818795, 1.2924592711349, 1.3669687907067, 1.4464837248482, 1.556463308124, 1.6899676161638, 1.7601514822835, 1.8260402529905, 1.8385670201281, 1.868626425126, 2.1659869823333, 2.324166597993, 2.4636475190109, 2.7830831561649, 3.142845716791, 3.2476217154365, 3.8741998698537, 3.9592519952242, 5.265806453317, 4.9910144274602, 1.5957247992325, 4.2811334701681, 14919.410887803 ];
//	const <chromosomesFactor = #[1, 1.0248818404643, 1.2586989312271, 1.3039238578163, 1.3777202708052, 1.4566257978907, 1.5662480524924, 1.7029500665129, 1.7650631334069, 1.8390163889117, 1.8462117858074, 1.8621374094106, 2.1641997484794, 2.3218601681945, 2.4309688587862, 2.7585778581012, 3.0697700147583, 3.1923592004677, 4.2153713518124, 3.9547570730079, 5.1787069346401, 4.85825415617, 1.6052664523139, 4.1980065842769, 15043.190355483];
	const <lengthAtOneMs = 69.236283611;//in hours
	const <sCompany = #["ftdna-illumina", "23andme", "23andme-exome-vcf", "decodeme"];
  //base vectors (last bit sets base pair indicator)
	const <a = #[1,0,0,0,0];
	const <c = #[0,1,0,0,0];
	const <g = #[0,0,1,0,0];
	const <t = #[0,0,0,1,0];
	const <e = #[0,0,0,0,0];
	const <p = #[0,0,0,0,1]; //base pair indicator
	classvar <workingSCompany = #[1];//the companies working with this software (so far)

	*calcPosition{//normalize all positions on chromosomes to the longest (the first) by chromosomesFactor of chromosomesFactor
		arg chromosome, position;
		var factor = 0.0, outPos = 0.0;
		switch(chromosome.asSymbol,
			\1,{factor = chromosomesFactor[0]},
			\2,{factor = chromosomesFactor[1]},
			\3,{factor = chromosomesFactor[2]},
			\4,{factor = chromosomesFactor[3]},
			\5,{factor = chromosomesFactor[4]},
			\6,{factor = chromosomesFactor[5]},
			\7,{factor = chromosomesFactor[6]},
			\8,{factor = chromosomesFactor[7]},
			\9,{factor = chromosomesFactor[8]},
			\10,{factor = chromosomesFactor[9]},
			\11,{factor = chromosomesFactor[10]},
			\12,{factor = chromosomesFactor[11]},
			\13,{factor = chromosomesFactor[12]},
			\14,{factor = chromosomesFactor[13]},
			\15,{factor = chromosomesFactor[14]},
			\16,{factor = chromosomesFactor[15]},
			\17,{factor = chromosomesFactor[16]},
			\18,{factor = chromosomesFactor[17]},
			\19,{factor = chromosomesFactor[18]},
			\20,{factor = chromosomesFactor[19]},
			\21,{factor = chromosomesFactor[20]},
			\22,{factor = chromosomesFactor[21]},
			\X,{factor = chromosomesFactor[22]},
			\Y,{factor = chromosomesFactor[23]},
			\MT,{factor = chromosomesFactor[24]}
		);
		^outPos = round(position.asFloat*factor);//round to the next possible slot
	}
	
	*convertChromosome{//convert funky names of chromosomes to valid numbers
		arg chrom;
		switch(chrom,
			\X, {^23},
			\Y, {^24},
			\MT,{^25},
			"X", {^23},
			"Y", {^24},
			"MT",{^25},
			{^chrom.asInt}
		);
	}
	
	*baseToVec{//calculate unique vectors on basis of the base given
		arg base;
		var out = [[],[]];
		if(base.isArray, {//unwrap items in Array
			base.do({
				arg item, i;
				if(item.notNil, {
					out.put(i, this.baseToVec(item));
				});
			});
			^out;
		},{
			if(this.isBasePair(base),{//if it's a base pair calc
        switch(base,
					\AA, {^a+p},
					\AC, {^(a-c)+p},
					\AG, {^(a-g)+p},
					\AT, {^(a-t)+p},
					\CC, {^c+p},
					\CG, {^(c-g)+p},
					\CT, {^(c-t)+p},
					\GG, {^g+p},
					\GT, {^(g-t)+p},
					\TT, {^t+p},
				);
			},{
				if(this.isBase(base),{//if it's a single base, return
          switch(base,
						\A, {^a},
						\C, {^c},
						\G, {^g},
						\T, {^t}
					);
				},{
					if(base == \none,{//if it's unknown, return empty
						^e;
					});
				});
			});
		});
	}


	*createResolverForPair{//for known base pairs, create a resolver
		arg base;
		base = base.asSymbol;
		switch(base,
			\AC,{^[a+p, c+p]},
			\AG,{^[a+p, g+p]},
			\AT,{^[a+p, t+p]},
			\CG,{^[c+p, g+p]},
			\CT,{^[c+p, t+p]},
			\GT,{^[g+p, t+p]},
			{^[e, e]}
		);
	}

	*isUnknownBasePair{//check if it's an unknown pair
		arg base;
		if(unknownBasePair.includes(base.asSymbol),{//if the base is part of the unknown base pairs
			^true;
		},{
			^false;
		});
	}

	*isBase{//check if it's a single base
		arg base;
		if(baseSingle.includes(base.asSymbol),{//if the base is part of the unknown base pairs
			^true;
		},{
			^false;
		});
	}
	
	*isBasePair{//check if it's a (known) base pair
		arg base;
		if(basePair.includes(base.asSymbol),{//if the base is part of the known base pairs
			^true;
		},{
			^false;
		});
	}
  *hasResolver{//check if the SNP has a resolver
    arg base;
    if(base.size>2, {
      ^true;
    },{
      ^false;
    });
  }
}
