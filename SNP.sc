SNP{//TODO: use vectorial represntations only, strip chromosome (redundant, as stored in SNPDict already), 
  var <chromosome;//chromosome as Symbol
  var <vecChromosome;//corresponding number
  var <id;//rsid of the SNP
  var <position;//position of SNP on own chromosome
  var <base;//base of the SNP (single or pair)
  var <vecBase;//vectorial representation of the base (pair)
  var <>resolver;//the other (two) combination(s) of the position
  var <vecResolver;//vectorial representation of the other combination(s)

  *new{
    arg aChromosome, aPosition = 0, aId, aBase, aResolver;
    ^super.new.init(aChromosome, aPosition, aId, aBase, aResolver);
  }

  init{
    arg aChromosome, aPosition, aId, aBase, aResolver;
    chromosome = aChromosome.asSymbol;
    vecChromosome = SNPInfo.convertChromosome(chromosome);
    position = aPosition.asFloat;
    id = aId.asSymbol;
    base = aBase.asSymbol;
    resolver = aResolver.asArray;
    vecBase = SNPInfo.baseToVec(base);
    vecResolver = SNPInfo.baseToVec(resolver);
  }

  hasResolver{//check if this SNP has a resolver
    var resolved = true;
    if(resolver.isArray,{
      resolver.do({
        arg item,i;
        if(item==\none,{
          resolved = false
        });
      });
      ^resolved;
    },{
      if(resolver!=\none,{
        ^true;
      },{
        ^false;
      });
    });
  }

  updateResolver{//update a resolver and its vector representation
    arg newResolver;
    if(newResolver!=resolver,{
      resolver = newResolver;
      vecResolver = SNPInfo.baseToVec(resolver);
    });
  }
}
