package samples

func identicalIfConditions(cond bool)  {
	if cond {

	} else if cond {

	}

	a, b := 1, 2
    if a == b {
        //Empty
    } else if a == b { //NonCompliant
        //Empty
    }

    a, b := 1, 2
    if foo() {
        //Empty
    } else if foo() { //NonCompliant
        //Empty
    }

    tag := 1
    switch tag {
        case 0: fmt.Println("1")
        case 4, 5, 6, 7: fmt.Println("2")
        case 0: fmt.Println("3") //NonCompliant
        case 4, 5, 6, 7, 8: fmt.Println("2")
        case 4, 5, 6, 7: fmt.Println("2") //NonCompliant
        default: fmt.Println("default")
    }

    switch {
        case tag > 0: fmt.Println("1")
        case tag == 0: fmt.Println("2")
        case tag > 0: fmt.Println("3") //NonCompliant
        default: fmt.Println("default")
    }
}

func foo() bool {
    return true
}
