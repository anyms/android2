function findTree(el) {
	let currentEl = el;
	const treeList = [];
	let nodes = Array.prototype.slice.call( currentEl.parentElement.children );
	let nth = nodes.indexOf(currentEl) + 1;
	treeList.push({
		tagName: currentEl.tagName.toLowerCase(),
		nth: nth,
		attrs: attrToArray( currentEl.attributes )
	});

	while (true) {
		currentEl = currentEl.parentElement;
		nodes = Array.prototype.slice.call( currentEl.parentElement.children );
		nth = nodes.indexOf(currentEl) + 1;
		const parentTagName = currentEl.tagName.toLowerCase();
		treeList.push({
			tagName: parentTagName,
			nth: nth,
			attrs: attrToArray( currentEl.attributes )
		});
		if (parentTagName == "body") {
			break;
		}
	}
	treeList.reverse();

	return treeList;
}

function attrToArray(attrs) {
	const arr = [];
	for (let i = 0; i < attrs.length; i++) {
		arr.push({
			name: attrs[i].name,
			value: attrs[i].value
		});
	}
	return arr;
}

function toSelector(tree) {
	let s = "";

	for (let i = 0; i < tree.length; i++) {
		s += (tree[i].tagName + ":nth-child(" + tree[i].nth + ") ");
	}

	return s.trim();
}

function concatTagNames(tree) {
	let s = "";
	for (let i = 0; i < tree.length; i++) {
		s += (tree[i].tagName + " ")
	}
	return s.trim();
}

function concatAttrClassNames(attrs) {
	let s = "";
	for (let i = 0; i < attrs.length; i++) {
		if (attrs[i].name === "class") {
			s += (attrs[i].value + " ")
		}
	}
	return s.trim();
}

function concatTagNamesWithNth(tree) {
	let s = "";
	for (let i = 0; i < tree.length; i ++) {
		s += (tree[i].tagName + " " + tree[i].nth + " ")
	}
	return s.trim()
}


function findSimilar(tree, doc) {
	const founds = [];
	const otherEls = doc.querySelectorAll(tree[tree.length - 1].tagName);
	const tagString = concatTagNames(tree);

	for (let i = 0; i < otherEls.length; i++) {
		const otherTree = findTree(otherEls[i]);
		const otherTagString = concatTagNames(otherTree);
		// concatTagNamesWithNth(tree) !== concatTagNamesWithNth(otherTree) && 
		if (!isHidden(otherEls[i]) && tagString === otherTagString && concatAttrClassNames(tree[tree.length - 1].attrs) === concatAttrClassNames(otherTree[otherTree.length - 1].attrs)) {
			//console.log(tagString, otherTagString);
			// console.log(doc.querySelector(toSelector(otherTree)));
			founds.push(otherTree);
		}
	}

	return founds;
}

function isHidden(el) {
    return (el.offsetParent === null);
}


function bindClickEvent(doc, isIframe, iframeIndex) {
	doc.addEventListener("click", function(e) {
		if (!isHidden(e.target)) {
			const element = {};
			const treeList = findTree(e.target);
			const selector = toSelector(treeList);
		    const similar = findSimilar(JSON.parse(JSON.stringify(treeList)), doc);
		    element.tree = treeList;
		    element.selector = selector;
		    element.similar = similar;
		    element.isIframe = isIframe;
		    element.iframeIndex = iframeIndex;

			console.log(element);
		}
	});
}


const iframes = document.querySelectorAll("iframe");

for (let i = 0; i < iframes.length; i++) {
	if (iframes[i].contentDocument !== null) {
		bindClickEvent(iframes[i].contentDocument, true, i);
	}
}


bindClickEvent(document, false, -1);
