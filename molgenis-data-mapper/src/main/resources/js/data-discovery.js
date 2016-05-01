(function($, molgenis) {
	
	$(function() {
		$('#entityNames').select2();
		
		//automatic adjustment for the width
		var reports = $('.data-discovery-panel');
		var number = reports.size();
		var rowNumber = Math.floor(number / 3);
		var residue = number % 3;
		if(residue != 0){
			var width = 94/residue + '%';
			var index = rowNumber*3;
			$('.data-discovery-panel:eq('+index+'), .data-discovery-panel:gt('+index+')')
		}
		
		//automatic adjustment for the height
		for(var i = 0;i < rowNumber;i++){
			var index = (i + 1) * 3;
			var maxHeight = Math.max.apply(null, $('.data-discovery-panel:lt('+index+')').map(function(){
				return $(this).height();
			}).get());
			$('.data-discovery-panel:lt('+index+')').css('height', maxHeight + 100);
		}
	});
	
}($, window.top.molgenis = window.top.molgenis || {}));