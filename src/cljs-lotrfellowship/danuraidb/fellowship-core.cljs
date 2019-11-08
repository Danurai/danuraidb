(ns danuraidb.fellowship-core
  (:require
    [reagent.core :as r]
    [danuraidb.fellowship-view :as view]))
    
(r/render [view/page] (.getElementById js/document "fellowship"))