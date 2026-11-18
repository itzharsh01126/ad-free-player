(function(){
  if(window.__ytab) return; window.__ytab=1;
  var css=document.createElement('style');
  css.innerHTML='.video-ads,.ytp-ad-overlay-container,.ytp-ad-module,ytm-promoted-sparkles-web-renderer,ytm-companion-slot,ytd-ad-slot-renderer,ytm-ad-slot-renderer,ytm-promoted-video-renderer,ytm-statement-banner-renderer,ytm-brand-video-singleton-renderer{display:none!important}';
  document.head.appendChild(css);
  var mutedByMe=false;
  setInterval(function(){
    var p=document.querySelector('.html5-video-player');
    var v=document.querySelector('video.html5-main-video')||document.querySelector('video');
    var ad=p&&p.classList.contains('ad-showing');
    if(ad&&v){
      mutedByMe=true; v.muted=true; v.playbackRate=16;
      if(isFinite(v.duration)&&v.duration>0) v.currentTime=v.duration;
    } else if(v&&mutedByMe){ v.muted=false; v.playbackRate=1; mutedByMe=false; }
    var b=document.querySelector('.ytp-ad-skip-button,.ytp-ad-skip-button-modern,.ytp-skip-ad-button,.ytp-ad-skip-button-container button');
    if(b) b.click();
  },250);
})();
