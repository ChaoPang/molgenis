library(gdata)

ranks.productRanks = function(ranks){
	result <- NULL;
	biobankNames <- colnames(ranks);
	for(biobankName in biobankNames[-1]){
		list <- ranks[,biobankName];
		total <- NULL;
		for(name in variableNames){
			index <- which(variableNames == name);
			value <- list[index];
			total <- c(total, ranks.extractranks(value));
		}
		total[which(total == -1)] = ranks.biobankSize(which(biobankNames == biobankName));
		result <- c(result, log(prod(total)));
	}
	return (result);
};

ranks.biobankSize = function(index){
	biobankSize <- c(516,224,6174,353,75,119);
	return (biobankSize[index - 1]);
};

ranks.extractranks = function(str){
	if(is.na(str)) array <- NULL; 
	array <- strsplit(as.character(str), split=",");
	array <- as.numeric(array[[1]]);
	return(array);
};

ranks.compare = function(value1, value2){
	array1 <- ranks.extractranks(value1);
	array2 <- ranks.extractranks(value2);
	result <- NULL;
	if(length(array1) != length(array2)){
		if(length(array1) > length(array2)){
			array2 <- c(array2, rep(-1, length(array1) - length(array2)));
		}else{
			array1 <- c(array1, rep(-1, length(array2) - length(array1)));
		}
	}
	result <- cbind(array1, array2);
	return (result);
};


wilcoxTest1 <- "/Users/chaopang/Desktop/Variables_result/Evaluation/Wilcox-test/wilcoxTest1.csv";
wilcoxTest2 <- "/Users/chaopang/Desktop/Variables_result/Evaluation/Wilcox-test/wilcoxTest2.csv";
ranks1 <- read.csv(wilcoxTest1, sep=';');
ranks2 <- read.csv(wilcoxTest2, sep=';');

wilcox.test(ranks.productRanks(ranks1), ranks.productRanks(ranks2), paired=T, exact=T);

variableNames <- ranks1[,'Variables'];
biobankNames <- colnames(ranks1);

pairedRanks <- NULL;

for(biobankName in biobankNames[-1]){
	list1 <- ranks1[,biobankName];
	list2 <- ranks2[,biobankName];
	for(name in variableNames){
		index <- which(variableNames == name);
		value1 <- list1[index];
		value2 <- list2[index];
		pairedRanks <- rbind(pairedRanks, ranks.compare(value1, value2));
	}
}
