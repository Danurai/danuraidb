(ns danuraidb.lotrsolocore
  (:require 
    [reagent.core :as r]
    [danuraidb.lotrsolomodel :as model]
    [danuraidb.lotrsoloview :as view]))
    
;(r/render [pdeck] (.getElementById js/document "lotrsolo"))
(r/render [view/Page] (.getElementById js/document "lotrsolo"))