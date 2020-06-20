$(document).scroll(function(){
    var sctop=$(this).scrollTop();
    if(sctop > 35){
        $(".head").addClass("sha");
    }else{
        $(".head").removeClass("sha");
    }
});
