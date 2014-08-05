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

file <- "/Users/chaopang/Desktop/Variables_result/Evaluation/R-input/output.xlsx";
biobankData <- biobank.plot(file)

legend <- c("1"="blue", "2"="purple1", "3"="black", "4"="red", "5"="green4", "6"="darkblue")
linestyle <- c("1"="solid","2"="dotted", "3"="longdash","4"="F1", "5"="twodash", "6"="1F")
label <- c("FinRisk", "HUNT", "KORA", "MICROS", "NCDS", "Total");

output <- c("/Users/chaopang/Desktop/Variables_result/Evaluation/R-input/output-recall-precision.pdf") 
pdf(output)
ggplot() + ylab("Precision") + xlab("Recall") + ggtitle("Recall-Precision") + 
		theme(plot.title = element_text(lineheight=.8, face="bold")) + xlim(0, 1) + ylim(0, 1) + 
			geom_path(data=biobankData, aes(y = Precision, x=Recall, group=group, linetype=as.factor(group), colour=as.factor(group)), size = 0.5) + 
			geom_point(data=biobankData, aes(y = Precision, x=Recall, colour=as.factor(group), shape=as.factor(group))) + 
			scale_colour_manual("legend", values=legend, labels=label) + 
			scale_linetype_manual("legend", values=linestyle, labels=label) +
			scale_shape_manual("legend", values=c(1,1,2,1,1,2), labels=label) + 
			coord_fixed(ratio=1)
ggsave(output);


output <- c("/Users/chaopang/Desktop/Variables_result/Evaluation/R-input/output-roc.pdf") 
pdf(output)
ggplot() + ylab("Sensitivity") + xlab("Specificity") + ggtitle("Sensitivity-Specificity") + 
		theme(plot.title = element_text(lineheight=.8, face="bold")) + xlim(0, 1) + ylim(0, 1) + 
		geom_path(data=biobankData, aes(y = TPR, x=FPR, group=group, linetype=as.factor(group), colour=as.factor(group)), size = 0.5) + 
		geom_point(data=biobankData, aes(y = TPR, x=FPR, colour=as.factor(group), shape=as.factor(group))) + 
		scale_colour_manual("legend", values=legend, labels=label) + 
		scale_linetype_manual("legend", values=linestyle, labels=label) +
		scale_shape_manual("legend", values=c(1,1,2,1,1,2), labels=label) + 
		coord_fixed(ratio=1)
ggsave(output);