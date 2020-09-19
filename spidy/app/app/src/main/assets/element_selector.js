var _spidy = {};
_spidy.elements = [];
_spidy.others = {};
_spidy.canRecord = true;

_spidy.findTree = function(el) {
	var currentEl = el;
	var treeList = [];
	var nodes = Array.prototype.slice.call( currentEl.parentElement.children );
	var nth = nodes.indexOf(currentEl) + 1;
	treeList.push({
		tagName: currentEl.tagName.toLowerCase(),
		nth: nth,
		attrs: _spidy.attrToArray( currentEl, currentEl.attributes )
	});

	while (true) {
		currentEl = currentEl.parentElement;
		var parentTagName = currentEl.tagName.toLowerCase();
		if (parentTagName == "body") {
			break;
		}

		nodes = Array.prototype.slice.call( currentEl.parentElement.children );
		nth = nodes.indexOf(currentEl) + 1;
		treeList.push({
			tagName: parentTagName,
			nth: nth,
			attrs: _spidy.attrToArray( currentEl, currentEl.attributes )
		});
	}
	treeList.reverse();

	return treeList;
};

_spidy.attrToArray = function(el, attrs) {
	var arr = [];
	for (var i = 0; i < attrs.length; i++) {
		if (attrs[i].name == "href") {
			arr.push({
				name: attrs[i].name,
				value: el.href
			});
		} else if (attrs[i].name == "src") {
			arr.push({
				name: attrs[i].name,
				value: el.currentSrc
			});
		} else if (attrs[i].name == "action") {
			arr.push({
				name: attrs[i].name,
				value: el.action
			});
		} else {
			arr.push({
				name: attrs[i].name,
				value: attrs[i].value
			});
		}
	}
	return arr;
};

_spidy.toSelector = function(tree) {
	var s = "";

	for (var i = 0; i < tree.length; i++) {
		s += (tree[i].tagName + ":nth-child(" + tree[i].nth + ") ");
	}

	return s.trim();
};

_spidy.concatTagNames = function(tree) {
	var s = "";
	for (var i = 0; i < tree.length; i++) {
		s += (tree[i].tagName + " ");
	}
	return s.trim();
};

_spidy.concatAttrClassNames = function(attrs) {
	var s = "";
	for (var i = 0; i < attrs.length; i++) {
		if (attrs[i].name === "class") {
			s += (attrs[i].value + " ");
		}
	}
	return s.trim();
};

_spidy.concatTagNamesWithNth = function(tree) {
		var s = "";
		for (var i = 0; i < tree.length; i ++) {
			s += (tree[i].tagName + " " + tree[i].nth + " ");
		}
		return s.trim();
};


_spidy.findSimilar = function(tree, doc, iframeIndex, currentEl) {
	var founds = [];
	var otherEls = doc.querySelectorAll("body " + tree[tree.length - 1].tagName);
	var tagString = _spidy.concatTagNames(tree);

	for (var i = 0; i < otherEls.length; i++) {
		var otherTree = _spidy.findTree(otherEls[i]);
		var otherTagString = _spidy.concatTagNames(otherTree);
		if (_spidy.concatTagNamesWithNth(tree) !== _spidy.concatTagNamesWithNth(otherTree) && !_spidy.isHidden(otherEls[i]) && tagString === otherTagString && _spidy.concatAttrClassNames(tree[tree.length - 1].attrs) === _spidy.concatAttrClassNames(otherTree[otherTree.length - 1].attrs) && currentEl.children.length === otherEls[i].children.length) {
			var obj = {selector: "body " + _spidy.toSelector(otherTree), iframeIndex: iframeIndex, elIndex: 0, attrs: _spidy.attrToArray(otherEls[i], otherEls[i].attributes)};
			var els = doc.querySelectorAll(obj.selector);
			for (var j = 0; j < els.length; j++) {
				if (els[j] === otherEls[i]) {
					obj.elIndex = j;
					founds.push(obj);
					break;
				}
			}
		}
	}

	return founds;
};

_spidy.isHidden = function(el) {
    return (el.offsetParent === null);
};


_spidy.bindClickEvent = function(doc, iframeIndex) {
	(new Hammer(doc)).on("press", function(e) {
		if (!_spidy.isHidden(e.target) && _spidy.canRecord) {
			if (_spidy.elements.length != 0) {
				for (var i = 0; i < _spidy.elements.length; i++) {
					var el = doc.querySelectorAll(_spidy.elements[i].selector)[_spidy.elements[i].elIndex];
					el.style.border = "0px solid yellow";
				}
			}
			_spidy.elements = []

			var treeList = _spidy.findTree(e.target);
			var selector = _spidy.toSelector(treeList);
			var allEls = document.querySelectorAll(selector);
			var currentElIndex = 0;
			for (let i = 0; i < allEls.length; i++) {
				if (allEls[i] === e.target) {
					currentElIndex = i;
					break;
				}
			}

			_spidy.elements.push({selector: "body " + selector, iframeIndex: iframeIndex, elIndex: currentElIndex, attrs: _spidy.attrToArray(e.target, e.target.attributes)});
			_spidy.others.doc = doc;
			_spidy.others.tree = treeList;
			_spidy.others.iframeIndex = iframeIndex;

			spidy.confirmElements(JSON.stringify(_spidy.elements));
			e.target.style.border = "1px solid yellow";
        }
	}, false);
};


_spidy.addSimilarElements = function() {
	var s = "";
	var similar = _spidy.findSimilar(
		JSON.parse(JSON.stringify(_spidy.others.tree)), 
		_spidy.others.doc,
		_spidy.elements[0].iframeIndex,
		_spidy.others.doc.querySelectorAll(_spidy.elements[0].selector)[_spidy.elements[0].elIndex]
	);
	for (var i = 0; i < similar.length; i++) {
		var tmp = document.createElement("div");
		var el = _spidy.others.doc.querySelectorAll(similar[i].selector)[similar[i].elIndex];
		tmp.appendChild(el.cloneNode(true));
		s += (tmp.innerHTML + "\n\n");
		el.style.border = "1px solid yellow";
		_spidy.elements.push(similar[i]);
	}
	return s === "" ? "there is no similar elements" : s;
};

_spidy.looksGood = function() {
	return "looks, good! total " + _spidy.elements.length + " element(s) found.";
};

_spidy.getSelectedElements = function() {
	if (_spidy.elements.length === 0) {
		return null;
	} else {
		return JSON.stringify(_spidy.elements);
	}
};


_spidy.findNearestElement = function(tagName) {
	var currentEl = _spidy.others.doc.querySelectorAll(_spidy.elements[0].selector)[_spidy.elements[0].elIndex];
	var currentElHolder = currentEl;
	var tagNameToFound = tagName.toLowerCase();
	var foundEl = null;
	while(true) {
		var isFound = false;
		var children = currentElHolder.querySelectorAll("*");
		for (var i = 0; i < children.length; i++) {
			if (children[i].tagName.toLowerCase() == tagNameToFound && children[i] !== currentEl && !_spidy.isHidden(children[i])) {
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

	if (foundEl !== null) {
		var treeList = _spidy.findTree(foundEl);
		var selector = _spidy.toSelector(treeList);
		var allEls = document.querySelectorAll(selector);
		var currentElIndex = 0;
		for (let i = 0; i < allEls.length; i++) {
			if (allEls[i] === foundEl) {
				currentElIndex = i;
				break;
			}
		}
		_spidy.elements.pop();
		_spidy.elements.push({selector: "body " + selector, iframeIndex: _spidy.others.iframeIndex, elIndex: currentElIndex, attrs: _spidy.attrToArray(foundEl, foundEl.attributes)});
		spidy.confirmElements(JSON.stringify(_spidy.elements));
		_spidy.others.tree = treeList;
		currentEl.style.border = "none";
		foundEl.style.border = "1px solid yellow";
	} else {
		spidy.showToast("No element exist with '" + tagNameToFound + "' tag name.");
		spidy.confirmElements(JSON.stringify(_spidy.elements));
		return "No element exist with '" + tagNameToFound + "' tag name.";
	}

	var tmp = document.createElement("div");
	tmp.appendChild(foundEl.cloneNode(true));
	return tmp.innerHTML;
};

// _spidy.iframes = document.querySelectorAll("iframe");

// for (var i = 0; i < _spidy.iframes.length; i++) {
// 	if (_spidy.iframes[i].contentDocument !== null) {
// 		_spidy.bindClickEvent(_spidy.iframes[i].contentDocument, i);
// 	}
// }


_spidy.bindClickEvent(document, -1);
