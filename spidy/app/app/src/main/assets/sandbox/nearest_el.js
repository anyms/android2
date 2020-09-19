var findNearestElement = function(tagName) {
	var currentEl = document.querySelectorAll("div.r")[0];
	var currentElHolder = currentEl;
	var tagNameToFound = tagName.toLowerCase();
	var foundEl = null;
	while(true) {
		var isFound = false;
		var children = currentElHolder.querySelectorAll("*");
		for (var i = 0; i < children.length; i++) {
			if (children[i].tagName.toLowerCase() == tagNameToFound && children[i] !== currentEl) {
				isFound = true;
				foundEl = children[i];
				break;
			}
		}
		if (isFound || currentElHolder.tagName.toLowerCase() === "body") {
			break;
		}
		currentElHolder = currentElHolder.parentElement;
	}

	return foundEl;
};