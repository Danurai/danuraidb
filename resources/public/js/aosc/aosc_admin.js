let _aosccards;
let $resultdata = $('<div>');
let $table = $('<table class="table table-sm">');
let $thead = $('<thead>');
let $tbody = $('<tbody>');
let _aoscfilter = {"setnumber":5}

$('#results').append($resultdata);
$('#results').append($table);
  
$table.append($thead);
$table.append($tbody);
  
$resultdata.html('results pending...');


$.getJSON("/aosc/api/data/cards", function (data) {
  $('#datasrcinfo').html('<b>Source:&nbsp;</b>' +  data.source);
  delete data.source;
  $('#aoscdata').val(JSON.stringify(data));
  _aosccards=TAFFY(data.hits.hits.map(c => c._source).map(card => $.extend({"setnumber": card.set[0].number},card)));
  write_table_aosccards();
});


$('#filter')
  .val(JSON.stringify(_aoscfilter))
  .on('change',function() {
    _aoscfilter = JSON.parse($(this).val());
    $('.btn-group-toggle').trigger('change');
  });
  
$('.btn-group-toggle').on('change',  function () {
  switch ($(this).find('input:checked')[0].id) {
    case "tbltags":
      write_table_tags();
      break;
    case "tblcorners":
      write_table_corners();
      break;
    case "tblsubjects":
      write_table_subjects();
      break;
    case "tblalliances":
      write_table_alliances();
      break;
    default:
      write_table_aosccards();
  }
});

$('#copydata').on('click', function () {
  $('#aoscdata').select();
  document.execCommand("copy");
  document.getSelection().removeAllRanges();
});

/* Table 1 - Card Images */

function write_table_aosccards() {
  var ccount = 0
  var mcount = 0;
  var x;
  var blocked = false;
  $thead.html('<tr><th>id</th><th>Name</th><th>default</th><th>finish</th><th>filename</th><th>local img</th><th>server img</th></tr>');
  $tbody.empty();
  let firstresult =  _aosccards(_aoscfilter).first();
  if (firstresult) { 
    $.get('https://assets.warhammerchampions.com/card-database/cards/' + firstresult.skus.filter(sku=>(sku.default))[0].id + '.jpg').fail(blocked = true)
    _aosccards(_aoscfilter).each(function (c) {
      ccount++;
      x=0;
      $.each(c.skus,function (id, sku) {
        if (sku.finish == "matte" && sku.default == true && sku.lang == "en") {
          x++;
          $tbody.append(write_row(c, sku, blocked));
        }
      });
      if (x>1) {mcount++}
    })
  }
  $resultdata.html('Cards: ' + ccount + ' with Multiples: ' + mcount);
}

function write_row(c, sku, blocked) {
  return '<tr>'
    + '<td>' + c.id + '</td>'
    + '<td>' + c.name + '</td>'
    + '<td>' + sku.default + '</td>'
    + '<td>' + sku.finish + '</td>'
    + '<td>' + sku.id + '.jpg</td>'
    + '<td><img width="50px" src="/img/aosc/cards/' + sku.id + '.jpg"></img></td>'
    + (blocked ? '<td>Blocked</td>' : '<td><img width="50px" src="https://assets.warhammerchampions.com/card-database/cards/' + sku.id + '.jpg"></img></td>')
    + '</tr>';
}
    
/* Table 2 tags */
function write_table_tags() {
  var imgnames = [].concat.apply([],_aosccards(_aoscfilter).map(c => c.tags))
    .map(c=>"tag_"+c.toLowerCase()+".png")
    .filter((ele,idx,a) => a.indexOf(ele) == idx);
  write_icon_table(imgnames);
}

/* Table 3 Corners */
function write_table_corners() {
  // map all corners into one array
  // filter out numbers and o x O X 
  // map to string e.g. quest_unit quest_unit_grot
  // filter unique values
  var imgnames = [].concat.apply([],_aosccards(_aoscfilter).map(c => c.corners))
    .filter(c=>c.value.match(/^[0-9OXox]{1}/)==null)
    .map(c=>"quest_"+c.value.toLowerCase()+(typeof c.qualifier != 'undefined' ? '_'+c.qualifier.toLowerCase() : '')+".png")
    .filter((e,i,a) => a.indexOf(e) == i);
  write_icon_table(imgnames);
}

/* Table 4 subjects */
function write_table_subjects() {
  var imgnames = _aosccards(_aoscfilter)
    .map(c=>c.subjectImage)
    .filter(c=>(typeof c != "undefined"))
    .map(c => "subject_"+c+".png")
    .filter((c,i,a)=>a.indexOf(c)==i);
  write_icon_table(imgnames);
}

/* Table 5 etc */
function write_table_alliances() {
  var imgnames = _aosccards(_aoscfilter)
    .map(c=>"alliance_"+c.alliance.toLowerCase()+".png")
    .filter((c,i,a)=>a.indexOf(c)==i);
  imgnames.push("alliance_unaligned.png");
  write_icon_table(imgnames);
}

function write_icon_table(imgnames) {
  var iconpath = "https://assets.warhammerchampions.com/card-database/icons/";
  var fname = "";
  $thead.html('<tr><th>Filename</th><th>Local</th><th>Server</th></tr>');
  $tbody.empty();
  $.each(imgnames,function (id,t) {
    $tbody.append('<tr>'
      + '<td>' + t + '</td>'
      + '<td><img src="/img/aosc/icons/' + t + '"></img></td>'
      + '<td><img src="' + iconpath + t + '"></img></td>'
      + '</tr>');
  });
}