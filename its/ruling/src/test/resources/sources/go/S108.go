// Copyright S108
package samples

func foo() {
	for { } //Compliant, FN due to the fact that loop are not mapped to LoopTree

	n := 3
	for n < 10 { } //Compliant, this is equivalent to a while loop and not reported by the rule

	for i := 0; i < 10; i++ { } //NonCompliant

	for i := 0; i < 10; i++ {
        //Compliant, this comment is inside
	}

	tag := 1

	switch tag { } //NonCompliant

	cond := false

    if cond { }  //NonCompliant
}

func bar() {} //Compliant for rule 108 (but will trigger rule S1186)
