
function whuw_markdown( str ) {
    /*
        **...** bold
        \n newline
        [...] Attack Action
        :xxx: Symbol
    */
    return str
            .replace(/\*{2}(.*?)\*{2}/g, '<b>$1</b>')
            .replace(/\*(.*?)\*/g, '<em>$1</em>')
            .replace(/\\n/g, '<br />')
            .replace(/\:(\w+)\:/g, '<i class="icon-$1"></i>')
            .replace(/\-\((.*?)\)\-/g, '<div class="action-effect">$1</div>')
            .replace(/\s(\-|[0-9])\s\-/g, ' $1 ')
            .replace(/\[(.*?)\]/g,'<div class="h5 rounded action">$1</div><br />');
}

function restrictions( formats, card ) {
    let valid = {};
    
    Object.keys( formats ).forEach( f => valid[ f ] = "Y");

    if (card.factionId == 1) {
        Object.keys( formats ).forEach( f => {
            // format: card_restriction: wave, forsaken, restricted
            let res = formats[ f ].card_restriction;
            let wave = true;
            if (typeof res.wave != 'undefined') {
                wave = res.wave.includes( Math.floor( card.id / 1000 ) )    // Check reprints.
                if (typeof card.duplicates != 'undefined') {
                    card.duplicates.forEach( d => {
                        if (wave == false && res.wave.includes( Math.floor( d / 1000) )) wave = "duplicated"
                    });
                }
            }
            let forsaken = typeof res.forsaken != 'undefined' 
                ? res.forsaken.includes ( card.id )
                : false;
            let restricted = typeof res.restricted != 'undefined' 
                ? res.restricted.includes ( card.id )
                : false;
            valid[ f ] = forsaken ? "F" : restricted ? "R" : wave == "duplicated" ? "Y+" : wave == true ? "Y" : "N";
        });
    }
    return valid;
}

export { whuw_markdown, restrictions }