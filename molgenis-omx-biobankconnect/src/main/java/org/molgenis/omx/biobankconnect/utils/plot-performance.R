# TODO: Add comment
# 
# Author: chaopang
###############################################################################

library(gdata)
library(ggplot2)


biobank.plot <- function(file) {
	biobanks <- sheetNames(file);
	aggregateData <- NULL
	for(biobankName in biobanks){
		biobankData <- read.xls(file, sheet=which(biobanks == biobankName))
		group <- rep(which(biobanks==biobankName), nrow(biobankData))
		biobankData <- cbind(biobankData, group)
		aggregateData <- rbind(aggregateData, biobankData)
	}
	return(aggregateData)
}

file <- "/Users/chaopang/Desktop/Variables_result/Evaluation/R-input/output-roc.xlsx";
biobankData <- biobank.plot(file)

legend <- c("1"="blue", "2"="purple1", "3"="#56B4E9", "4"="red", "5"="green4")
linestyle <- c("1"="solid","2"="dotdash", "3"="longdash","4"="F1", "5"="twodash")
label <- c("NCDS", "HUNT", "FinRisk", "KORA", "MICROS");

tpr <- c();
fpr <- c();

for(i in 1:5){
	indices <- which(biobankData["group"] == i)
	index <- indices[length(indices)]
	biobankData[index, "TPR"]
	tpr <- c(tpr, biobankData[index, "TPR"]);
	fpr <- c(fpr, biobankData[index, "FPR"]);
}

d=data.frame(fpr=fpr, tpr=tpr, xend=rep(1,5), yend=rep(1,5))


output <- c("/Users/chaopang/Desktop/Variables_result/Evaluation/R-input/output-roc.pdf") 
pdf(output)
ggplot() + ylab("True positive rate") + xlab("False positive rate") + ggtitle("ROC") + 
		theme(plot.title = element_text(lineheight=.8, face="bold")) + xlim(0, 1) + ylim(0, 1) + 
		geom_path(data=biobankData, aes(y = TPR, x=FPR, group=group, linetype=as.factor(group), colour=as.factor(group)), size = 0.5) + 
		#geom_point(data=biobankData, aes(y = TPR, x=FPR, colour=as.factor(group), shape=as.factor(group))) + 
		scale_colour_manual("legend", values=legend, labels=label) + 
		scale_linetype_manual("legend", values=linestyle, labels=label) +
		#scale_shape_manual("legend", values=c(1,2,3,4,5), labels=label) + 
		coord_fixed(ratio=1) + 
		geom_segment(aes(x = 0, y = 0, xend = 1, yend = 1), linetype=2) + 
		geom_segment(data=d, mapping=aes(x=fpr, y=tpr, xend=rep(1,5), yend=rep(1,5)), color=c("1"="blue", "2"="purple1", "3"="#56B4E9", "4"="red", "5"="green4"), linetype="dotted")
		

ggsave(output);