let _factions, _sets, _cards, _factionMembers, _waveCounts;
let url = "/whuw/api/yauwdb";

$.get(url, data => {
    _factions = data.factions;
    _factionMembers = data.factionMembers;
    _sets = data.sets;
    _cards = data.cards;
    let _waveIds = new Set( Object.keys(_cards).map( id => id[0] ) );
    _waveCounts = {};
    _waveIds.forEach( id => {
        _waveCounts[id] = Object.keys( _cards ).filter( c => c[0] == id ).length;
    });

    $('#faction').on('change', function () {
        let faction = this.value;
        viewFighters( faction );
        viewFactionCards( faction );
        viewSetCards( faction );
    });

    function viewFighters ( faction ) {
        let factionName = _factions[ faction ].name;
        let members = _factionMembers[ factionName ];
        let factionFighters = $('<div class="whuw__faction-fighters d-flex flex-wrap justify-content-center mb-2"></div>');
        $('#faction-members')
            .empty()
            .append ( factionFighters );
        if ( typeof members != 'undefined' ) {
            members.forEach( ( m, i ) => {
                factionFighters.append( factionMember( factionName, m, i + 1 ) );
            })
        }
    }

    function viewFactionCards( faction ) {
        let factionId = _factions[ faction ].id;
        let factionCards = $('<div class="whuw__faction-cards d-flex flex-wrap justify-content-center"></div>')
        $('#faction-cards')
            .empty()
            .append(factionCards);
        Object.values(_cards).filter( c => c.factionId == factionId ).forEach( c => {
            factionCards.append( cardElement( c ) );
        });
    }

    function viewSetCards( faction ) {
        let factionId = _factions[ faction ].id;
        let sets = new Set( Object.values(_cards).filter( c => c.factionId == factionId ).map( c => c.setId ) );
        $('#set-cards').empty();
        sets.forEach( setId => {
            let set = Object.values( _sets ).filter( s => s.id == setId )[0];
            let setWrapper = $('<div></div>');
            let cardsWrapper = $('<div class = "d-flex flex-wrap justify-content-center"></div>');
            $('#set-cards').append( setWrapper );
            setWrapper.append(`<div class="text-light mb-2 text-center"><b>${set.displayName}</b></div>`);
            setWrapper.append( cardsWrapper );
            Object.values( _cards ).filter( c => c.setId == setId ).forEach( c => {
                cardsWrapper.append( cardElement( c, false) );
            });
        });
    }

    function factionMember( factionName, m, i ) {
        let srcBase = `/img/whuw/fighters/${factionName}-${i}.webp`;
        let srcInspired = `/img/whuw/fighters/${factionName}-${i}-inspired.webp`;
        let innerHTML = `<div class="whuw__fighter-cards">
                            <div class="whuw__cards-swap-trigger"><i class="fas fa-exchange-alt fa-lg"></i></div>
                            <div class="whuw__cards-wrapper">
                                <img src="${srcBase}" alt="${m}" class="whuw__fighter-card whuw__fighter-card-active" data-toggle = "modal" data-target = "#card-modal">
                                <img src="${srcInspired}" alt="${m}" class="whuw__fighter-card" data-toggle = "modal" data-target = "#card-modal">
                            </div>
                        </div>`
        return innerHTML; 
    }
    
    function cardElement( card, nameSet = true ) {
        let wave = String(Math.floor(card.id / 1000));
        let set = Object.values(_sets).filter( s => s.id == card.setId )[0];
        let faction = Object.values(_factions).filter( f => f.id == card.factionId)[0];
        let setIcons = (typeof card.duplicates != 'undefined'
            ? card.duplicates.map( id => {
                    let dupeset = Object.values(_sets).filter( s => s.id == _cards[ id ].setId )[0];
                    return `<img class="icon-sm${id != card.id ? ' icon-grey' : ''}" src="/img/whuw/icons/${dupeset.name}-icon.png" title="${dupeset.displayName}">` })
                .join(' ')
            : `<img class="icon-sm" src="/img/whuw/icons/${set.name}-icon.png" title="${set.displayName}">` );
        let gloryIcon = '<img class="icon m-1" src="/img/whuw/icons/total_glory.png">';
        let gloryIcons = (typeof card.glory != 'undefined' 
            ? `<div class="d-flex justify-content-center">${gloryIcon.repeat( card.glory )}</div>`
            : '')
        return `<div class="whuw__card d-flex flex-column m-1 border border-light rounded bg-secondary">
                <div style = "flex: 1 1 auto;" class="p-2" data-toggle="modal" data-target="#card-modal" data-cardid="${card.id}">
                    <div class="d-flex justify-content-between mb-2" style="align-items: flex-start;">
                        <img class="icon" src="/img/whuw/icons/type_${card.type.toLowerCase()}.png" title="${card.type}">
                        <img class="icon" src="/img/whuw/icons/${faction.name}-icon.png" title="${faction.displayName}">
                    </div>
                    <h5 class="text-center">${card.name}</h5>
                    <div class="text-center small">${whuw_markdown( card.rule )}</div>
                </div>
                ${gloryIcons}
                <div class="p-2">
                    <div><span>Sets: </span>${setIcons}</div>
                    <div class="small d-flex">
                        <img class="mr-1 ml-auto" style="width: 16px" src="/img/whuw/icons/wave-${wave.padStart( 2, '0')}-icon.png" title="Wave ${wave}">
                        <div>#${card.id % 1000}/${_waveCounts[ wave ]}</div>
                    </div>
                </div>
            </div>`
    }

    function warhammerUnderworldsCardURL( card ) {
        let setPrefix = [ '-', 'S', 'L', 'NV', 'NVPU', 'COD', 'BG', 'BGGIFTPACK', 'ARENAMORTIS', 'DIRECHASM' ];
        let cycle = Math.round( card.id / 1000 );
        let setCardId = String( card.id % 1000 );
        let cardImg =  setPrefix[ cycle ] + ( cycle == 1 ? setCardId.padStart( 3, '0' ) : cycle == 2 ? setCardId.padStart( 2, '0' ) : setCardId );
        return `https://images.warhammerunderworlds.com/en/${cardImg}.png`;
    }
    function cardImgElement( card, nameSet = true ) {
        let imgname = String(card.id).padStart(5, '0');
        let src = warhammerUnderworldsCardURL( card );
        let set = Object.values(_sets).filter( s => s.id == card.setId )[0];
        let setNameDisplay = nameSet ? 'inherit' : 'none';
        let alt = card.id + ': ' + card.name;

        return `<div style="width: 180px; padding: 0.3rem;" >
                    <img class = "img-fluid whuw__card" src = "${src}" data-toggle = "modal" data-target = "#card-modal" alt = "${alt}">
                    <div style = "text-align: center; font-size: 0.6rem; color: #ddd; display: ${setNameDisplay};"><b>${set.displayName}</b></div>
                </div>`
    }

    $('#faction-members')
        .on('click', '.whuw__cards-swap-trigger', function () {
            $('#faction-members').find('.whuw__fighter-card').toggleClass('whuw__fighter-card-active')
        });
    
    $('#card-modal').on('show.bs.modal', function (evt) {
        //let card = _cards[ $(evt.relatedTarget).data('card-id') ];
        //let imgname = String(card.id).padStart(5, '0');
        //let src = `/img/whuw/assets/cards/${imgname}`;
        let src = evt.relatedTarget.src;
        if (typeof src == 'undefined') {
            $('#card-modal').find('.modal-body').html(
                cardElement( _cards[ $(evt.relatedTarget).data('cardid') ] )
            );    
        } else {
            $('#card-modal').find('.modal-body').html(
                `<picture><img class="img-fluid" src="${src}"></picture>`   //<source srcset = "${src}_xs.webp">
            );
        }
    });

    
    $('#faction')[0].value = `Khagra's Ravagers`;
    $('#faction').trigger('change');
});

function whuw_markdown( str ) {
    /*
        **...** bold
        \n newline
        [...]
    */
    return str
            .replace(/\*{2}(.*?)\*{2}/g, '<b>$1</b>')
            .replace(/\*(.*?)\*/g, '<em>$1</em>')
            .replace(/\\n/g, '<br />')
            .replace(/\[(.+)\]/g,'<div class="p-2 text-white rounded" style="background-color: black">$1</div>');
}