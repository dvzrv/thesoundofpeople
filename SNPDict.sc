SNPDict{//TODO: only use array representations in snpDict (get rid of SNPs in there)
	const <emptyBasePair = #[\none,\none];
	const <emptyBase = #[\none];
	var <snpDict;
	var <snpArr;
	var <positionLookup;
	var <unknownLookup;
	var <unknownArr;
	var <unknownToDelete;
	var <>length;
	var <positions = 0;
	var <unknowns = 0;
	var <userID;

	*new{
		arg aLength, aUserID = 0;
		^super.new.init(aLength, aUserID);
	}

	init{
		arg aLength, aUserID;
		userID = aUserID.asInt;
		length = aLength.asInt;
		snpDict = Dictionary.new(length);
		//snpArr = Array.new(length);
		positionLookup = Dictionary.new(length);
		unknownLookup = Dictionary.new(length);
		//unknownArr = Array.new(length);
	}
	
	updatePositionLookup{//sort the dictionary's position lookup table when updating a new position
		arg position, positionAdd;
		if(positionAdd,{//add or remove a position in the lookup table
			positionLookup.add(position.asFloat -> true);
		},{
			positionLookup.removeAt(position.asFloat);
		});
	}

	updateUnknownLookup{//sort the dictionary's unknown lookup table when updating a SNP with unknown resolver
		arg position, positionAdd;
		if(positionAdd,{
			unknownLookup.add(position.asFloat -> true);
		},{
			unknownLookup.removeAt(position.asFloat);
		});
	}

	orderLookup{//order the lookup tables (position and unknown) and store them to Arrays (for sequential access)
		arg table;
		switch(table,
			0,{
				snpArr = positionLookup.order;
				("Done sorting position lookup table. Positions: "++snpArr.size.asString).postln;
				^true;
			},
			1,{
				unknownArr = unknownLookup.order;
				("Done sorting unknown lookup table. Unknowns: "++snpArr.size.asString).postln;
				^true;
			},
			2,{
				snpArr = positionLookup.order;
				("Done sorting position lookup table. Positions: "++snpArr.size.asString).postln;
				unknownArr = unknownLookup.order;
				("Done sorting unknown lookup table. Unknowns: "++snpArr.size.asString).postln;
				unknownLookup = nil;
				positionLookup = nil;
				^true;
			},{
				("Unknown operation on sorting lookup tables: "++table.asString).postln;
				^false;
			}
		);
	}

	storeSNP{//store a combo to the snpDict and the lookup tables when reading from opensnp.org -file
		arg snp, position;
		var positionHolder, new = 0;//the SNP holder in snpDict
		if(snpDict.includesKey(position.asSymbol), {//if there is an entry already, add this one after it
			snpDict.at(position.asSymbol).add(snp.chromosome -> snp);
			new = -1;
		},{//else create the first one in this slot
			positionHolder = Dictionary.new(25);	
			positionHolder.add(snp.chromosome -> snp);
			snpDict.put(position.asSymbol, positionHolder);
			this.updatePositionLookup(position, true);
			new = 1;
			positions = positions+1;
		});
		if(snp.hasResolver.not,{//if the SNP has no resolver, update the unknown lookup table
			this.updateUnknownLookup(position, true);
		});
		^new;
	}

	storeSNPFromFile{//store a combo to the snpDict and the lookup tables when reading from own file
		arg snp, position;
		var positionHolder, new = 0;//the SNP holder in snpDict
		if(snpDict.includesKey(position.asSymbol), {//if there is an entry already, add this one after it
			snpDict.at(position.asSymbol).add(snp.chromosome -> snp);
		},{//else create the first one in this slot
			positionHolder = Dictionary.new(25);
			positionHolder.add(snp.chromosome -> snp);
			snpDict.put(position.asSymbol, positionHolder);
		});
	}
	
	initLookupFromFile{//init lookup table from file with size given
		arg posLookup, unLookup;
		snpArr = Array.new(posLookup);
		unknownArr = Array.new(unLookup);
	}

	addPositionLookupFromFile{//put a position to the position lookup table when reading from own file
		arg posArr, posSNP;
		snpArr.add(posSNP);
	}

	addUnknownLookupFromFile{//put a position to the position lookup table when reading from own file
		arg posArr, posSNP;
		unknownArr.add(posSNP);
	}
	
	updateResolver{//update a SNP resolver at a given position
		arg position, id, resolver;
		var resolverLeft;
		if(resolver!=SNPInfo.emptyBasePair && resolver!=SNPInfo.emptyBase,{//if the resolver is none, don't do anything
			snpDict.at(position.asSymbol).do({
				arg snpItem;
				if(id==snpItem.id, {
					snpItem.resolver_(resolver);
					("Resolver updated.").postln;
					^true;
				});
			});
//			resolverLeft = this.noneResolverAtPosition(position);
//			if(resolverLeft == false,{//if there are no unknowns left, delete position from lookup table
//				unknownArr.removeAt(unknownArr.indexOf(position.asFloat));
//			});
//			resolverLeft = nil;
		},{
			("Not updating position "++position++". Resolver is none.").postln;
			^false;
		});
	}

	deleteSNP{//delete a SNP at a given position
		arg snp, position;
		var snpCounter = 0, deleted = false;
		snpDict.at(position.asSymbol).do({
			arg snpItem, i;
//			if(snpItem.notNil, {
				snpCounter = snpCounter + 1;
				if(snp.id == snpItem.id{
					snpItem.removeAt(position.asSymbol);
					snpCounter = snpCounter - 1;
					deleted = true;
				});
//			});
		});
		if(snpCounter < 1,{//delete the complete position if there is no SNP left
			snpDict.removeAt(position.asSymbol);
			this.sortPositionLookup(-1, position);
			this.sortUnknownLookup(-1, position);
		});
		^deleted;
	}

	noneResolverAtPosition{//return a Dictionary of Chromosome -> SNP pairs that don't have a resolver or false if none found
		arg position;
		var noneResolver = Dictionary.new(25), noneResolverCount = 0;
		if(snpDict.includesKey(position.asSymbol),{//if the position exists, check it
			snpDict.at(position.asSymbol).keysValuesDo({
				arg chromosome, snp;
				if(snp.hasResolver.not,{
					noneResolver.add(chromosome -> snp);
					noneResolverCount = noneResolverCount + 1;
				});
			});
			if(noneResolverCount>0,{
				^noneResolver;
			},{
				^false;
			});
		},{
			^false;
		});
	}

	snpAtPosition{//return a Dictionary of chromosome -> SNP pairs at a given position, or false if none available
		arg position;
		if(snpDict.includesKey(position.asSymbol),{//check if the key is there
			^snpDict.at(position.asSymbol);//return the Dictionary at position
		},{
			^false;
		});
	}
	
	distanceToNextPosition{//calculate the distance from current position, to the one following
		arg position;
		var now; 
		if(position==0.0,{//if it's the beginning, return first position later on
			now = -1;
		},{
			now = snpArr.indexOf(position.asFloat);
		});
		switch(now,
			nil,{^position},
			snpArr.size-1,{^0},
			-1,{^snpArr[0]},
			{^snpArr[(snpArr.indexOf(position.asFloat)+1)]-position;}
		);
	}

	queryStatistics{//query statistics about this SNPDict
		//TODO: shortest/longest/mean distance between SNPs
		//TODO: unknown/total SNPs
		//TODO: number of SNPs per chromosome
		//TODO: unknown/total SNPs per chromosome
		//TODO: total positions
	}
}
