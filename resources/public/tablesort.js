$(document).ready(function () {
  
  $('th.sortable').on('click',function () {
    var desc = false;
    var tbl = $(this).closest('table')[0];
    var switching=true;
    var n=this.cellIndex;
    var switchcount=0;
    
    while (switching) {
      var switching=false;
      var rows=tbl.rows;
      
      for (var i=1;i<(rows.length-1);i++) {
        var doswitch=false;
        var a=$(rows[i]).find('td')[n];
        var b=$(rows[i+1]).find('td')[n];
        var val_a = (a.textContent != "" ? a.textContent : a.title);
        var val_b = (b.textContent != "" ? b.textContent : b.title);
        if (desc) {
          if ($.isNumeric(val_a) && val_b == "X") {
            doswitch = true;
          } else if ($.isNumeric(val_a) && val_b == "-") {
            doswitch = false;
          } else if ($.isNumeric(val_a) && $.isNumeric(val_b)) {
            doswitch = (parseInt(val_a) < parseInt(val_b));
          } else {
            doswitch = (val_a < val_b);
          }
        } else {
          if ($.isNumeric(val_a) && val_b == "X") {
            doswitch = false;
          } else if ($.isNumeric(val_a) && val_b == "-") {
            doswitch = true;
          } else if ($.isNumeric(val_a) && $.isNumeric(val_b)) {
            doswitch = (parseInt(val_a) > parseInt(val_b));
          } else {
            doswitch = (val_a > val_b);
          }
        }
        if (doswitch) {
          rows[i].parentNode.insertBefore(rows[i+1],rows[i]);
          switching=true;
          switchcount++;
        }
      }
      // if no switches, set switching true and reverse direction
      if (switchcount == 0 && desc == false) {
        desc=true;
        switching=true;
      }
    }
    // add styling
    $(tbl).find('.caret').remove();
    $(this).append('<span class="caret"><i class="fas fa-caret-' + (desc ? "down" : "up") + '"></span>');
  })
  
});