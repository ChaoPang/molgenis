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

legend <- c("1"="blue", "2"="purple1", "3"="purple1", "4"="red", "5"="green4", "6"="green4","7"="darkblue", "8"="darkblue")
linestyle <- c("1"="solid","2"="dotted", "3"="dotted", "4"="longdash","5"="F1", "6"="F1","7"="twodash", "8"="twodash")
label <- c("FinRisk", "HUNT", "HUNT-knock-out", "KORA", "MICROS", "MICROS-knock-out", "NCDS","NCDS-knock-out");

output <- c("/Users/chaopang/Desktop/Variables_result/Evaluation/R-input/output.pdf") 
pdf(output)
ggplot() + xlab("Precision") + ylab("Recall") + ggtitle("Recall-Precision") + 
		theme(plot.title = element_text(lineheight=.8, face="bold")) + xlim(0, 1) + ylim(0, 1) + 
			geom_path(data=biobankData, aes(x = Precision, y=Recall, group=group, linetype=as.factor(group), colour=as.factor(group)), size = 0.5) + 
			geom_point(data=biobankData, aes(x = Precision, y=Recall, colour=as.factor(group), shape=as.factor(group))) + 
			scale_colour_manual("legend", values=legend, labels=label) + 
			scale_linetype_manual("legend", values=linestyle, labels=label) +
			scale_shape_manual("legend", values=c(1,1,2,1,1,2,1,2), labels=label) + 
			coord_fixed(ratio=1)
ggsave(output);

