function Spidy() {
    this.canRecord = true;
    this.node = {isAll: false};
    var self = this;
    this.init = function() {
        (new Hammer(document)).on("press", function(e) {
            if (!self.isHidden(e.target) && self.canRecord) {
                if (self.node.selector !== undefined) {
                    var el = document.querySelectorAll(self.node.selector)[self.node.elIndex];
                    el.style.border = "none";

                    if (self.node.isAll) {
                        var tree = self.generateTree(el);
                        var similar = self.createSimilarElements(tree, el);
                        for (var i = 0; i < similar.length; i++) {
                            document.querySelectorAll(similar[i].selector)[similar[i].elIndex].style.border = "none";;
                        }
                    }
                }

                var tree = self.generateTree(e.target);
                var selector = self.createSelector(tree);
                var allEls = document.querySelectorAll(selector);
                var currentElIndex = 0;

                for (var i = 0; i < allEls.length; i++) {
                    if (allEls[i] === e.target) {
                        currentElIndex = i;
                        break;
                    }
                }

                self.node.selector = "body " + selector;
                self.node.elIndex = currentElIndex;
                self.node.isAll = false;
                spidycom.confirmElements(JSON.stringify(self.node), JSON.stringify(self.attrToArray(e.target, e.target.attributes)));
                e.target.style.border = "1px solid yellow";
            }
        });
    };
}

Spidy.prototype.generateTree = function(el) {
    var currentEl = el;
	var tree = [];
	var nodes = Array.prototype.slice.call( currentEl.parentElement.children );
	var nth = nodes.indexOf(currentEl) + 1;
	tree.push({ tagName: currentEl.tagName.toLowerCase(), nth: nth });

	while (true) {
		currentEl = currentEl.parentElement;
		var parentTagName = currentEl.tagName.toLowerCase();
		if (parentTagName == "body") {
			break;
		}

		nodes = Array.prototype.slice.call( currentEl.parentElement.children );
		nth = nodes.indexOf(currentEl) + 1;
		tree.push({ tagName: parentTagName, nth: nth });
	}
	tree.reverse();

	return tree;
};


Spidy.prototype.createSelector = function(tree) {
    var s = "";

	for (var i = 0; i < tree.length; i++) {
		s += (tree[i].tagName + ":nth-child(" + tree[i].nth + ") ");
	}

	return s.trim();
};


Spidy.prototype.createTagId = function(tree) {
    var s = "";
	for (var i = 0; i < tree.length; i++) {
		s += (tree[i].tagName + " ");
	}
	return s.trim();
};


Spidy.prototype.createTagIdWithNth = function(tree) {
    var s = "";
	for (var i = 0; i < tree.length; i ++) {
		s += (tree[i].tagName + " " + tree[i].nth + " ");
	}
	return s.trim();
};

Spidy.prototype.isHidden = function(el) {
    return (el.offsetParent === null);
};

Spidy.prototype.createSimilarElements = function(tree, currentEl) {
    var founds = [];
    var allEls = document.querySelectorAll("body " + tree[tree.length - 1].tagName);
    var tagId = this.createTagId(tree);

    for (var i = 0; i < allEls.length; i++) {
        var otherTree = this.generateTree(allEls[i]);
        var otherTagId = this.createTagId(otherTree);

        if (this.createTagIdWithNth(tree) !== this.createTagIdWithNth(otherTree) && !this.isHidden(allEls[i]) && otherTagId === tagId && currentEl.children.length === allEls[i].children.length) {
            var vEl = {selector: "body " + this.createSelector(otherTree), elIndex: 0};
            var els = document.querySelectorAll(vEl.selector);
            for (var j = 0; j < els.length; j++) {
                if (els[j] === allEls[i]) {
                    vEl.elIndex = j;
                    els[j].style.border = "1px solid yellow";
                    founds.push(vEl);
                    break;
                }
            }
        }
    }

    return founds;
};

Spidy.prototype.createAllElements = function(node) {
    var els = document.querySelectorAll(node.selector);
    var vEls = [];
    for (var i = 0; i < els.length; i++) {
        vEls.push({selector: node.selector, elIndex: i});
    }
    return vEls;
};

Spidy.prototype.switchToNearestElement = function(node, tagName) {
    var currentEl = document.querySelectorAll(node.selector)[node.elIndex];
    var currentElHolder = currentEl;
	var tagNameToFound = tagName.toLowerCase();
    var foundEl = null;
    
    while(true) {
		var isFound = false;
		var children = currentElHolder.querySelectorAll("*");
		for (var i = 0; i < children.length; i++) {
			if (children[i].tagName.toLowerCase() == tagNameToFound && children[i] !== currentEl && !this.isHidden(children[i])) {
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
		var treeList = this.generateTree(foundEl);
		var selector = this.createSelector(treeList);
		var allEls = document.querySelectorAll(selector);
		var currentElIndex = 0;
		for (let i = 0; i < allEls.length; i++) {
			if (allEls[i] === foundEl) {
				currentElIndex = i;
				break;
			}
        }
        node.selector = "body " + selector;
        node.elIndex = currentElIndex;
        
		spidycom.confirmElements(JSON.stringify(node), JSON.stringify(this.attrToArray(foundEl, foundEl.attributes)));
		currentEl.style.border = "none";
		foundEl.style.border = "1px solid yellow";
	} else {
		spidycom.showToast("No element exist with '" + tagNameToFound + "' tag name.");
		spidycom.confirmElements(JSON.stringify(node), JSON.stringify(this.attrToArray(currentEl, currentEl.attributes)));
		return "No element exist with '" + tagNameToFound + "' tag name.";
	}

	var tmp = document.createElement("div");
	tmp.appendChild(foundEl.cloneNode(true));
	return tmp.innerHTML;
};


Spidy.prototype.attrToArray = function(el, attrs) {
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
				value: attrs[i].value + ""
			});
		}
	}
	return arr;
};


Spidy.prototype.addSimilarElements = function(node) {
    node.isAll = true;
    var el = document.querySelectorAll(node.selector)[node.elIndex];
    this.createSimilarElements(this.generateTree(el), el);
    return JSON.stringify(node);
};


Spidy.prototype.getElementsCount = function(node) {
    if (node.isAll) {
        var el = document.querySelectorAll(node.selector)[node.elIndex];
        var similar = this.createSimilarElements(this.generateTree(el), el);
        return similar.length + 1;
    } else {
        return 1;
    }
};


Spidy.prototype.getVElements = function(node) {
    var els = [{selector: node.selector, elIndex: node.elIndex}];
    if (node.isAll) {
        var el = document.querySelectorAll(node.selector)[node.elIndex];
        var similar = this.createSimilarElements(this.generateTree(el), el);
        
        for (var i = 0; i < similar.length; i++) {
            els.push({selector: similar[i].selector, elIndex: similar[i].elIndex});
        }
    }
    return els;
};

Spidy.prototype.getElements = function(node) {
    var els = [];
    var el = document.querySelectorAll(node.selector)[node.elIndex];
    els.push(el);
    if (node.isAll) {
        var similar = this.createSimilarElements(this.generateTree(el), el);
        
        for (var i = 0; i < similar.length; i++) {
            els.push(document.querySelectorAll(similar[i].selector)[similar[i].elIndex]);
        }
    }
    return els;
};


var spidy = new Spidy();
