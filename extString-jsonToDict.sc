JSONReader {

	*new {
		arg path, keys_as_strings = false;
		var string = File(path,"r").readAllString;
		var identity_dict = super.new.prepareForJSonDict(string).interpret.as(IdentityDictionary);

		identity_dict.dopostln;

		if(keys_as_strings.not,{
			^this.idToD(identity_dict,false);
		},{
			^this.idToD(identity_dict);
		});
	}

	*idToD {
		arg ident_dict, toD = true;
		var new_dict;

		//"ident_dict: %".format(ident_dict).postln;
		if(toD,{
			new_dict = Dictionary.new;
		},{
			new_dict = IdentityDictionary.new;
		});

		ident_dict.keysValuesDo({
			arg key, val;
			var new_key;

			/*			"key: %\t\tval:%".format(key,val).postln;
			val.class.postln;
			"".postln;*/

			if(toD,{
				new_key = key.asString;
			},{
				new_key = key.asSymbol;
			});

			case
			{val.isKindOf(SequenceableCollection).and(val.isString.not)}{
				new_dict.put(new_key,this.process_seq_col(val,toD));
			}
			{val.isKindOf(Dictionary)}{
				new_dict.put(new_key,this.idToD(val,toD));
			}
			{
				new_dict.put(new_key,this.checkItem(val));
			};
		});
		^new_dict;
	}

	*checkItem {
		arg item;
		var return = nil;
		item.postln;
		item.class.postln;

		case
		{item == "inf"}{return = inf}
		{item == "-inf"}{return = -inf}
		{return = item};

		return.postln;
		return.class.postln;
		"".postln;
		^return;
	}

	*process_seq_col {
		arg seq_col, toD;
		^seq_col.collect({
			arg item;
			var return;
			case
			{item.isKindOf(Dictionary)}{
				return = this.idToD(item,toD);
			}
			{item.isKindOf(SequenceableCollection) && item.isString.not}{
				return = this.process_seq_col(item,toD);
			}{
				return = this.checkItem(item);
			};
			return;
		});
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