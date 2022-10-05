package band

type Header interface {
	Print() string
}

type Common struct {
	field1 int
	field2 int
	field3 int
}

type Bar struct {
	field1      int
	field2      int
	field3      int
	barSpecific string
}

func (b Bar) Print() string {
	return "This is a Bar"
}

type Foo struct {
	field1      int
	field2      int
	field3      int
	fooSpecific string
}

func (f Foo) Print() string {
	return "This is a Foo"
}

func ProcessHeader(header Header) Common {
	switch input := header.(type) {
	case Foo:
		return Common{
			field1: input.field1,
			field2: input.field2,
			field3: input.field3,
		}
	case Bar:
		return Common{ // Compliant
			field1: input.field1,
			field2: input.field2,
			field3: input.field3,
		}
	default:
		panic("...")
	}
}

func PrintsExpectedMessage(header Header) bool {
	printed := header.Print()
	switch printed {
	case "This is a Bar":
		tmp := true
		return tmp
	case "This is a Foo":
		tmp := true // Noncompliant
		return tmp
	default:
		return false
	}
}

func UnrelatedTest(common Common) bool {
	sum := 0
	if common.field1 > common.field2 {
		sum += common.field1 + common.field2
		common.field3 = sum * sum
	} else if common.field1 == common.field2 { // Noncompliant
		sum += common.field1 + common.field2
		common.field3 = sum * sum
	}
	return sum > 42
}

func SwitchWithLongExpression() bool {
	switch header.Print() {
	case "This is a Bar":
		tmp := true
		return tmp
	case "This is a Foo":
		tmp := true // Noncompliant
		return tmp
	default:
		return false
	}
}
