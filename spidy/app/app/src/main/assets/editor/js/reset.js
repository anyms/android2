var resetWorkspace = function() {

    function DOMLang(sel) {
        this.els = document.querySelectorAll(sel);
    }

    DOMLang.prototype.css = function(obj, index) {
        var keys = Object.keys(obj);
        for (var i = 0; i < keys.length; i++) {
            if (index === undefined || index === null) {
                for (var j = 0; j < this.els.length; j++) {
                    this.els[j].style[keys[i]] = obj[keys[i]];
                }
            } else {
                this.els[index].style[keys[i]] = obj[keys[i]];
            }
        }
        return this;
    };

    DOMLang.prototype.html = function(s, index) {
        if (index === undefined || index === null) {
            for (var j = 0; j < this.els.length; j++) {
                this.els[j].innerHTML = s;
            }
        } else {
            this.els[index].innerHTML = s;
        }
        return this;
    };

    DOMLang.prototype.on = function(evt, callback) {
        for (var j = 0; j < this.els.length; j++) {
            this.els[j].addEventListener(evt, callback, false);                
        }
        return this;
    };

    DOMLang.prototype.each = function(callback) {
        for (var j = 0; j < this.els.length; j++) {
            callback(this.els[j]);            
        }
        return this;
    };

    function $(sel) {
        return new DOMLang(sel);
    }


    $(".blocklyTreeRow").css({
        "border": "2px solid #212529",
        "color": "#ccc"
    }).on("mouseup", function() {
        $(".blocklyTreeRow").each(function(el) {
            var style = window.getComputedStyle(el, "");
            var bgColor = style.getPropertyValue("background-color");
            el.style["border-color"] = "#212529";
            el.querySelector(".blocklyTreeLabel").style["color"] = "#ccc";
        });

        var element = this;
        setTimeout(function() {
            var style = window.getComputedStyle(element, "");
            var bgColor = style.getPropertyValue("background-color");

            if (bgColor.startsWith("rgb(")) {
                transBgColor = bgColor.substring(0, bgColor.length - 1) + ", 0.3)";
                element.style["background-color"] = transBgColor;
                element.style["border-color"] = bgColor;
                //element.querySelector(".blocklyTreeLabel").style["color"] = bgColor;
            }
        }, 1);
    });


    $(".blocklyToolboxDiv.blocklyNonSelectable").css({
        "background-color": "#272E32"
    });
    


    $(".blocklyTreeIcon.blocklyTreeIconNone")
        .css({
            "padding-left": "10px",
            "padding-right": "5px"
        })
        .html('<img src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABgAAAAYCAYAAADgdz34AAABGUlEQVR42u2VTQrCMBCFC25dqlsRO1l4BNEbiIIewiuIuNGjCJq2V/HvBl5AwaoLrQXjjFXxB2omILow8CAkL+9LJqS1rG+1vQMV5VmJdz7ykJcVrrxCMnBgjhrEQWiOPOSlNSzIbpDLXiAycOwWaoT9VSTq0xjNwZy8RmU6ONDEABUn8hiF4w5rGBC+A5BnL+0qK3zbz2cCCUuN8LPwkhe0Rn/3LnSui0OZL4aeKD2HvoxL0WaUB6a3IAwJpSi/AJ7HJYw5AF+3PHdacQDKRNbPtI+f4A/QAWwMAGsOYGYAmHI+FT0+QHQZPxw7TUdmAHw1FCneW3ChgQuPGuHkqZs9uAgSdxLfOPyhXNGdTFDbiyZUc3ZZPtFOox6oFCtDMGYAAAAASUVORK5CYII=" width="100%" height="100%" class="source-structure-item__image">', 0)
        .html('<img src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABgAAAAYCAYAAADgdz34AAABKklEQVR42mNgGAWkgHe5DN7vcxieAPF/MvHjdzkMnjgtACmgwHAYfoTPgv/UwNSw4P2LEgbuD7kMqWB+LkPC/QQGDiD7LbUs+P8hmyHtcSEDJ5B9+1kaA9f7bIZYavoAhC+B9HzKZ1CH6j1CqgVXsBj6CophfAeQvg95DMakxkHD/3oGFmCSW4Fs+Ls8Bh0QRrJkHlTfYpItAPFBlnzIYVgOMxycVxAWXAW5/GMWgwqQ/ZucVIRsiSKa4WD5V1kMPMAUtIfsZAo0uAQmB7IELfyfAfFnSvIBPFjAPgllYAYmxSXUymgvYYa/zWbQhvkEHPG5DCsptgBooBPMcJBl6HGCIwmT5IPL7/MYHJEMh0c8FNO8LCLbAtoW16DKgkJLHr3NYvAYrbpJAgDpRKvvhodqXQAAAABJRU5ErkJggg==" width="100%" height="100%" class="source-structure-item__image">', 1)
        .html('<img src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABgAAAAYCAYAAADgdz34AAAAPUlEQVR42mNgGAXEAqbyvf+phYepBUMfMJfvPUqN4GEu33d4NBWNWkAjC0DJiyrJtGLvodHierS4HgWoAADGYkpJrXhuDwAAAABJRU5ErkJggg==" width="100%" height="100%" class="source-structure-item__image">', 2)
        .html('<img src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABgAAAAYCAYAAADgdz34AAAAlElEQVR42mNgGAwgzXgma5b07J4s6VmvQThTZmZXPUM9C9UsyJKZ1Q00+D8yBllCskGZ0jO9gZqfoBsGx1KzrLIkZ9rglJee9ThTZpYnbpcCFeDR/B9keKbkbFu8aqRnPsJnwX9q4MFjAdGJYNBaMBoHo3FA2zgYGhYMyTh4TLkFeIprUGVBmSUzH2XJzPBgGAWkAACIl0patjPi5QAAAABJRU5ErkJggg==" width="100%" height="100%" class="source-structure-item__image">', 3)
        .html('<img src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABgAAAAYCAYAAADgdz34AAABN0lEQVR42mNgGAVg8J9RV2xWB02M1tJaxaYrPnMpEP+nuuF64ou4dcVmbgMZTsCCeiY98Rm+QMULgAqvAvFHqCYQfQUkriM+ywcUFDAdOmKzxXXFZpyBGY5sAcgskJlw44GaTyIrxIVB6mB6gPw76PJIciD+aQY0AaIwPj3ocqMWjFpAkgX/dCVmlNPKgp9AHIWcbalpwRddiVme6OUCtSx4oSs52xhbwUMVC3TEZirjKtmoYoG6yFxePfHZTroSM3NAEawnPitfT2JGiJ70bBmKLNCRmO6gIz5zE5D9g1jHIFvwRVd81n5wsQ2soYAu6gaKrQLi+1gM+KQrPmMvkJ4MUgukZ0ItfoJhAcggHYlZYcZSM7lwhauO5AwToOVbQQ4AlfcqKpPYcanVFptuDXRk7fBoDgAA1ssafUSeuhQAAAAASUVORK5CYII=" width="100%" height="100%" class="source-structure-item__image">', 4)
        .html('<img src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABgAAAAYCAYAAADgdz34AAABc0lEQVR42mNgGHFA02zSYZpaoGE6+T/VDXVwqGcBGtwGxM9BFmiaTekCiVHR1ZMaQQaj4W7qWWA2+QHE0IlWasZTbaAWvKK6BSDDtcwm2UIteEpNCxqwBFE79SwwmVSFZPBTdZNJHVpa9WxUMVzbsk9I3XTye0jqmeSNLKeiMomdChlrShfIcKAlh2BiWqZTtIFimzVMp/RQZLi64WQpoEFfwRaYTLaGW2A8xRwo9g+Iv+uYTpUl3wLTSbOh4b4RM29M2QTx2aTZZKacKWpAA34D8V81s0n6GEFnMlEHJAfEf1QtpmqSU+asgbp+AU4fmk1aBFZjNmU1aUFjNtkEGsY/VU2mKeF0hEW/AlDND5BaTfNJFqSE/R5wsjSdPIGwT6dMhPr0AHHJ0mSKK8Tbkz/rmE8QJ6RexXCSKNCST2AHmUxyISbsT0NcNKmRjNL2NFEVCqjcVzGfxEesBSC1sLqCqNSjZTpVgtRUB9ID0jv8GgkAlfy3IYX3lvAAAAAASUVORK5CYII=" width="100%" height="100%" class="source-structure-item__image">', 5)
        .html('<img src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABgAAAAYCAYAAADgdz34AAAAMElEQVR42mNgGAVDAqwVCf9PKh5hFhCylObxQtWgGOEWkBLGoxaMWjAILBgFwwcAAGYfAR22BOoCAAAAAElFTkSuQmCC" width="100%" height="100%" class="source-structure-item__image">', 6)

};


resetWorkspace();