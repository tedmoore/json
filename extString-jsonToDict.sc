JSONReader {

	*new {
		arg path;
		var string = File(path,"r").readAllString;
		^super.new.prepareForJSonDict(string).interpret
	}

	getQuotedTextIndices {
		arg string, quoteChar = "\"";
		var quoteIndices;

		quoteIndices = string.findAll(quoteChar.asString);
		// remove backquoted chars
		quoteIndices = quoteIndices.select{|idx, i|
			string[idx-1] != $\\
		} ?? {[]};

		^quoteIndices.clump(2);
	}

	getUnquotedTextIndices {
		arg string, quoteChar = "\"";
		^((([-1] ++ this.getQuotedTextIndices(string,quoteChar).flatten ++ [string.size]).clump(2)) +.t #[1, -1])
	}

	getStructuredTextIndices {
		arg string;
		var unquotedTextIndices;

		unquotedTextIndices = this.getUnquotedTextIndices(string);
		unquotedTextIndices = unquotedTextIndices.collect{|idxs|
			this.getUnquotedTextIndices(string.copyRange(*idxs),$') + idxs.first
		}.flat.clump(2);

		^unquotedTextIndices
	}

	prepareForJSonDict {
		arg string;
		var newString = string.deepCopy;
		var idxs, nullIdxs;
		idxs = this.getStructuredTextIndices(newString);


		idxs.do{|pairs, i|
			Interval(*pairs).do{|idx|
				(newString[idx] == ${).if({newString[idx] = $(});
				(newString[idx] == $}).if({newString[idx] = $)});

				(newString[idx] == $:).if({
					[(idxs[i-1].last)+1, pairs.first-1].do{|quoteIdx|
						newString[quoteIdx] = $'
					}
				});
			}
		};

		// replace null with nil
		nullIdxs = newString.findAll("null");
		nullIdxs.do{|idx|
			idxs.any{|pairs| idx.inRange(*pairs)}.if({
				newString.overWrite("nil ", idx);
			})

		};

		^newString
	}
}