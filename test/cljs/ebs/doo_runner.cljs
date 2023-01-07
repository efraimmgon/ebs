(ns ebs.doo-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [ebs.core-test]))

(doo-tests 'ebs.core-test)

